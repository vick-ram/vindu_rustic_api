package org.example.data.db.config

object TsVectorManager {
    fun createAllTriggers() {
        tsVectorConfigs.forEach { config ->
            createTriggerFunction(config)
            createTrigger(config)
        }
    }

    private fun createTriggerFunction(config: TsVectorConfig) {
        val connection = DatabaseFactory.datasource.connection

        try {
            val statement = connection.createStatement()

            val weightExpression = config.searchColumns.joinToString(" || \n") { column ->
                val weight = config.weights[column] ?: "D"
                "setweight(to_tsvector('${config.language}', coalesce(new.$column, '')), '$weight')"
            }

            val functionSql = """
                CREATE OR REPLACE FUNCTION ${config.tableName}_tsvector_trigger() RETURNS trigger AS $$
                begin
                    new.${config.tsVectorColumn} :=
                        $weightExpression;
                    return new;
                end
                $$ LANGUAGE plpgsql;
            """.trimIndent()
            statement.execute(functionSql)
            println("Created trigger function for table: ${config.tableName}")
        } catch (e: Exception) {
            println("Error creating trigger function for ${config.tableName}: ${e.message}")
        } finally {
            connection.close()
        }
    }

    private fun createTrigger(config: TsVectorConfig) {
        val connection = DatabaseFactory.datasource.connection

        try {
            val statement = connection.createStatement()

            val triggerSql = """
                DO $$
                BEGIN
                    IF EXISTS(
                        SELECT 1 FROM pg_trigger
                        WHERE tgname = '${config.tableName}_tsvector_update'
                    )
                    THEN
                        DROP TRIGGER ${config.tableName}_tsvector_update ON ${config.tableName};
                    END IF;
                    
                    CREATE TRIGGER ${config.tableName}_tsvector_update BEFORE INSERT OR UPDATE OF ${
                config.searchColumns.joinToString(
                    ", "
                )
            } ON ${config.tableName} FOR EACH ROW EXECUTE FUNCTION ${config.tableName}_tsvector_trigger();
                    END $$;
            """.trimIndent()
            statement.execute(triggerSql)
        } catch (e: Exception) {
            println("Error creating trigger for ${config.tableName}: ${e.message}")
        } finally {
            connection.close()
        }
    }

    fun populateExistingData() {
        tsVectorConfigs.forEach { config ->
            populateTableData(config)
        }
    }

    private fun populateTableData(config: TsVectorConfig) {
        val connection = DatabaseFactory.datasource.connection
        try {
            val statement = connection.createStatement()

            val weightExpressions = config.searchColumns.joinToString(" ||\n") { column ->
                val weight = config.weights[column] ?: "D"
                "setweight(to_tsvector('${config.language}', coalesce($column, '')), '$weight')"
            }

            val updateSql = """
                UPDATE ${config.tableName} 
                SET ${config.tsVectorColumn} = $weightExpressions
                WHERE ${config.tsVectorColumn} IS NULL OR ${config.tsVectorColumn} = '';
            """.trimIndent()

            statement.executeUpdate(updateSql)

        } catch (e: Exception) {
            println("Error populating data for ${config.tableName}: ${e.message}")
        } finally {
            connection.close()
        }
    }

}