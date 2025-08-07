package org.example.utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import jakarta.mail.Authenticator
import jakarta.mail.Message
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
    private val fromEmail: String
) {
    private val session: Session = Session.getInstance(getSmtpProperties(), getAuthenticator())

    private fun getSmtpProperties(): Properties {
        val properties = mapOf(
            "mail.smtp.auth" to "true",
            "mail.smtp.starttls.enable" to "true",
            "mail.smtp.port" to smtpPort.toString(),
            "mail.smtp.connectiontimeout" to "5000", // 5s timeout
            "mail.smtp.timeout" to "5000", // 5s timeout
            "mail.smtp.writetimeout" to "5000"
        )

        return Properties().apply {
            putAll(properties)
        }
    }

    private fun getAuthenticator(): Authenticator  {
        return object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                return PasswordAuthentication(username, password)
            }
        }
    }

    suspend fun sendEmailAsync(to: String, subject: String, body: String) {
        withContext(Dispatchers.IO) {
            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(fromEmail))
                    setRecipient(Message.RecipientType.TO, InternetAddress(to))
                    setSubject(subject)
                    setText(body)
                }
                Transport.send(message)
                println("Email sent to $to")
            } catch (e: Exception) {
                println("Failed to send email: ${e.message}")
            }
        }
    }
}