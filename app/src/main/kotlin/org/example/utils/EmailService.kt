package org.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*

class EmailService(
    private val smtpHost: String,
    private val smtpPort: Int = 587,
    private val username: String,
    private val password: String,
    private val fromEmail: String,
    private val debugMode: Boolean = false
) {
    private val session: Session by lazy { createSession() }

    private fun createSession(): Session {
        return Session.getInstance(getSmtpProperties(), getAuthenticator())
            .apply {
                debug = debugMode
            }
    }

    private fun getSmtpProperties(): Properties {
        val properties = mapOf(
            "mail.smtp.host" to smtpHost,
            "mail.smtp.auth" to "true",
            "mail.smtp.starttls.enable" to "true",
            "mail.smtp.port" to smtpPort.toString(),
            "mail.smtp.connectiontimeout" to "5000",
            "mail.smtp.timeout" to "5000",
            "mail.smtp.writetimeout" to "5000"
        )

        return Properties().apply {
            putAll(properties)
        }
    }

    private fun getAuthenticator(): Authenticator {
        return object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                return PasswordAuthentication(username, password)
            }
        }
    }

    suspend fun sendEmail(to: String, subject: String, body: String, isHtml: Boolean = false, vararg cc: String): Result<Unit> = withContext(
        Dispatchers.IO) {
        try {
            val message = createMessage(to, subject, body, isHtml, *cc)
            Transport.send(message)
            Result.success(Unit)
        } catch (e: MessagingException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createMessage(
        to: String,
        subject: String,
        body: String,
        isHtml: Boolean,
        vararg cc: String
    ): MimeMessage {
        return MimeMessage(session).apply {
            setFrom(InternetAddress(fromEmail))
            setRecipient(Message.RecipientType.TO, InternetAddress(to))

            if (cc.isNotEmpty()) {
                setRecipients(
                    Message.RecipientType.CC,
                    cc.joinToString(",") { InternetAddress(it).toString()}
                )
            }

            setSubject(subject)
            if (isHtml) {
                setContent(body, "text/html; charset=utf-8")
            } else {
                setText(body)
            }
        }
    }
}