package org.example.utils.notifications

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.example.config.AppConfig
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.system.DispatchResult
import org.slf4j.LoggerFactory
import java.util.*

@Component
class EmailNotificationService @Inject constructor(private val config: AppConfig) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val session: Session by lazy { createSession() }

    private fun createSession(): Session {
        return Session.getInstance(getSmtpProperties(), getAuthenticator())
            .apply { debug = config.server.development }
    }

    private fun getSmtpProperties(): Properties {
        val properties = mapOf(
            "mail.smtp.host" to config.email.smtpHost,
            "mail.smtp.auth" to "true",
            "mail.smtp.starttls.enable" to "true",
            "mail.smtp.port" to config.email.smtpPort.toString(),
            "mail.smtp.connectiontimeout" to config.email.timeouts.connection.toString(),
            "mail.smtp.timeout" to config.email.timeouts.read.toString(),
            "mail.smtp.writetimeout" to config.email.timeouts.write.toString()
        )

        return Properties().apply {
            putAll(properties)
        }
    }

    private fun getAuthenticator(): Authenticator {
        return object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(config.email.username, config.email.password)
            }
        }
    }

    private fun buildMessage(to: String, subject: String, body: String, vararg cc: String): MimeMessage {
        return MimeMessage(session).apply {
            setFrom(InternetAddress(config.email.fromEmail))
            setRecipient(Message.RecipientType.TO, InternetAddress(to))

            if (cc.isNotEmpty()) {
                setRecipients(
                    Message.RecipientType.CC,
                    cc.joinToString(",") { InternetAddress(it).toString() }
                )
            }

            setSubject(subject)
            setContent(body, "text/html; charset=utf-8")
        }
    }

    fun send(notificationId: String ,to: String, subject: String, body: String, actionUrl: String?): DispatchResult {
        return try {
            Transport.send(buildMessage(to, subject, body))

            logger.info("Email sent for notification $notificationId to $to")

            DispatchResult(
                notificationId = notificationId,
                channel = "email",
                success = true,
                message = "Email sent for notification $notificationId"
            )
        } catch (e: Exception) {
            logger.error("Failed to send email for notification ${notificationId}", e)
            throw e
        }
    }
    private fun emailBody(body: String?): String {
        val htmlBody = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <title>Email</title>
        </head>
        <body style="margin:0; padding:0; background-color:#121212;">
          <table border="0" cellpadding="0" cellspacing="0" width="100%">
            <tr>
              <td align="center" bgcolor="#121212">
                <table border="0" cellpadding="0" cellspacing="0" width="600" 
                       style="max-width:600px; background-color:#1e1e1e; border-radius:8px; overflow:hidden;">
                  <!-- Header -->
                  <tr>
                    <td align="center" bgcolor="#0d6efd" style="padding:24px;">
                      <h1 style="margin:0; font-family:Arial, sans-serif; font-size:22px; color:#ffffff;">
                        Vindu Rustic
                      </h1>
                    </td>
                  </tr>
                  <!-- Body -->
                  <tr>
                    <td style="padding:30px; font-family:Arial, sans-serif; color:#e0e0e0; line-height:1.6;">
                      <h2 style="margin-top:0; color:#0d6efd;">Hi, %s</h2>
                      <p style="margin:0 0 16px;">
                        $body
                      </p>
                      <p style="margin:0 0 20px;">
                        Here’s a quick call-to-action for you:
                      </p>
                      <!-- Button -->
                      <table cellspacing="0" cellpadding="0" border="0" align="center" style="margin:20px auto;">
                        <tr>
                          <td align="center" bgcolor="#0d6efd" style="border-radius:4px;">
                            <a href="https://example.com" target="_blank" 
                               style="display:inline-block; padding:14px 28px; font-family:Arial, sans-serif; 
                               color:#ffffff; text-decoration:none; font-size:16px; font-weight:bold;">
                              Take Action
                            </a>
                          </td>
                        </tr>
                      </table>
                      <p style="margin:0;">
                        Need help? Just reply to this email or reach out at 
                        <a href="mailto:support@vindurustic.com" 
                           style="color:#0d6efd; text-decoration:none;">support@example.com</a>.
                      </p>
                    </td>
                  </tr>
                  <!-- Footer -->
                  <tr>
                    <td bgcolor="#1a1a1a" align="center" 
                        style="padding:20px; font-family:Arial, sans-serif; font-size:12px; color:#aaaaaa;">
                      <p style="margin:0;">
                        &copy; ${java.time.Year.now()} Vindu Rustic. All rights reserved.
                      </p>
                      <p style="margin:4px 0 0;">
                        1234 Business St, Nairobi, Kenya
                      </p>
                      <p style="margin:4px 0 0;">
                        <a href="https://example.com/unsubscribe" 
                           style="color:#777777; text-decoration:none;">Unsubscribe</a>
                      </p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.trimIndent()

        return htmlBody
    }
}
