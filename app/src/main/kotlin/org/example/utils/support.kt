package org.example.utils

fun generateResponse(userMessage: String): String {
    val message = userMessage.lowercase()

    return when {
        message.contains("hello") || message.contains("hi") || message.contains("hey") -> "Howdy! Welcome to Vindu Rustic Forge. How can we light up your day?"
        message.contains("goodbye") || message.contains("bye") || message.contains("see you") -> "Thanks for stopping by! Y'all come back now, you hear?"

        // Core Business Inquiries
        message.contains("what do you sell") || message.contains("what kind of") || message.contains("products") -> "We specialize in hand-crafted rustic lighting and traditional equipment. Think wrought iron chandeliers, mason jar pendants, barn-style sconces, and old-world tools like butter churns and cast iron cookware."
        message.contains("chandelier") || message.contains("pendant") || message.contains("sconce") -> "Our lighting is hand-forged with care. Are you looking for something for your kitchen, porch, or maybe a grand entryway? You can browse our full collection on the 'Lighting' page."

        // Materials & Craftsmanship (Important for this niche!)
        message.contains("material") || message.contains("made of") || message.contains("what is it made of") -> "We use authentic materials like reclaimed barn wood, hand-blown glass, wrought iron, and aged copper. Each piece has its own unique character and story."
        message.contains("handmade") || message.contains("crafted") || message.contains("artisan") -> "Yes! Every single item is crafted by skilled artisans right here in our workshop. We believe in keeping traditional craftsmanship alive."

        // Shipping & Policies
        message.contains("shipping") || message.contains("delivery") || message.contains("how long") -> "We carefully package every piece. Shipping typically takes 5-7 business days within the country. Due to the handmade nature, some items may have a longer lead time."
        message.contains("return") || message.contains("exchange") || message.contains("warranty") -> "We want you to love your piece. We offer a 30-day return policy on unused items. Our lighting also comes with a 1-year craftsmanship warranty."

        // Pricing
        message.contains("price") || message.contains("how much") || message.contains("cost") -> "Our prices vary based on the piece, size, and materials. You'll find the price listed on each product page. We focus on value and heirloom quality."

        // Contact & Location
        message.contains("contact") || message.contains("email") || message.contains("phone") -> "You can reach our friendly craftsfolk at support@therusticforge.com or by calling 555-202-1234. We're here Monday-Friday, 9am-5pm."
        message.contains("where are you") || message.contains("location") || message.contains("store") -> "Our workshop is located at 123 Old Mill Road, Smoky Mountain, TN. We'd love for you to visit us in person!"

        // Default fallback response
        else -> "That's a great question about our rustic goods! I'm still learning the trade. For detailed help, please email support@therusticforge.com and one of our human artisans will be happy to assist you."
    }
}

data class IncomingMessage(val message: String)
data class OutgoingMessage(val reply: String)