class ChatInterface {
    constructor() {
        this.isMobile = window.innerWidth <= 768;
        this.init();
    }

    init() {
        this.elements = {
            chatListSidebar: document.getElementById('chatListSidebar'),
            chatArea: document.getElementById('chatArea'),
            startConversationView: document.getElementById('startConversationView'),
            activeChatView: document.getElementById('activeChatView'),
            mobileOverlay: document.getElementById('mobileOverlay'),
            startChatBtnMobile: document.getElementById('startChatBtnMobile'),
            backButton: document.getElementById('backButton'),
            newChatBtn: document.getElementById('newChatBtn'),
            chatItems: document.querySelectorAll('.chat-item')
        };

        this.bindEvents();
        this.setupResponsiveBehavior();
    }

    bindEvents() {
        // Mobile start chat button
        this.elements.startChatBtnMobile.addEventListener('click', () => {
            this.openChatList();
        });

        // Back button in chat header
        this.elements.backButton.addEventListener('click', () => {
            this.closeActiveChat();
        });

        // Mobile overlay click
        this.elements.mobileOverlay.addEventListener('click', () => {
            this.closeChatList();
        });

        // New chat button
        this.elements.newChatBtn.addEventListener('click', () => {
            this.startNewChat();
        });

        // Chat item clicks
        this.elements.chatItems.forEach(item => {
            item.addEventListener('click', () => {
                this.selectChat(item);
            });
        });

        // Window resize handler
        window.addEventListener('resize', () => {
            this.handleResize();
        });

        // Send message on enter key
        const messageInput = document.querySelector('.message-input');
        const sendBtn = document.querySelector('.send-btn');
        
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });

        sendBtn.addEventListener('click', () => {
            this.sendMessage();
        });
    }

    setupResponsiveBehavior() {
        if (this.isMobile) {
            // On mobile, start with conversation view
            this.showStartConversationView();
        } else {
            // On desktop, start with chat list and start conversation view
            this.showStartConversationView();
        }
    }

    handleResize() {
        const wasMobile = this.isMobile;
        this.isMobile = window.innerWidth <= 768;

        if (wasMobile !== this.isMobile) {
            // View mode changed
            if (!this.isMobile) {
                // Switched to desktop - ensure sidebar is visible
                this.elements.chatListSidebar.classList.remove('mobile-open');
                this.elements.mobileOverlay.classList.remove('mobile-open');
            } else {
                // Switched to mobile - close sidebar if open
                this.closeChatList();
            }
        }
    }

    openChatList() {
        if (this.isMobile) {
            this.elements.chatListSidebar.classList.add('mobile-open');
            this.elements.mobileOverlay.classList.add('mobile-open');
        }
    }

    closeChatList() {
        if (this.isMobile) {
            this.elements.chatListSidebar.classList.remove('mobile-open');
            this.elements.mobileOverlay.classList.remove('mobile-open');
        }
    }

    selectChat(chatItem) {
        // Remove active class from all chat items
        this.elements.chatItems.forEach(item => {
            item.classList.remove('active');
        });

        // Add active class to selected chat
        chatItem.classList.add('active');

        // Show active chat view
        this.showActiveChatView();

        // Update chat header with selected chat info
        this.updateChatHeader(chatItem);

        // Close chat list on mobile
        if (this.isMobile) {
            this.closeChatList();
        }
    }

    showStartConversationView() {
        this.elements.startConversationView.classList.remove('hidden');
        this.elements.activeChatView.classList.add('hidden');
        
        // Remove active class from all chat items
        this.elements.chatItems.forEach(item => {
            item.classList.remove('active');
        });
    }

    showActiveChatView() {
        this.elements.startConversationView.classList.add('hidden');
        this.elements.activeChatView.classList.remove('hidden');
    }

    closeActiveChat() {
        if (this.isMobile) {
            this.showStartConversationView();
        }
    }

    updateChatHeader(chatItem) {
        const chatName = chatItem.querySelector('.chat-name').textContent;
        const chatAvatar = chatItem.querySelector('.chat-avatar img').src;
        
        const partnerAvatar = this.elements.activeChatView.querySelector('.partner-avatar img');
        const partnerName = this.elements.activeChatView.querySelector('.partner-name');
        
        partnerAvatar.src = chatAvatar;
        partnerName.textContent = chatName;
    }

    startNewChat() {
        // Implementation for starting a new chat
        console.log('Starting new chat...');
        // You would typically open a contact selection modal here
        
        // For demo purposes, show a temporary message
        alert('New chat feature would open a contact selection modal.');
    }

    sendMessage() {
        const messageInput = document.querySelector('.message-input');
        const messageText = messageInput.value.trim();

        if (messageText) {
            const messagesContainer = document.querySelector('.messages');
            
            const messageElement = document.createElement('div');
            messageElement.className = 'message sent';
            messageElement.innerHTML = `
                <div class="message-content">
                    ${this.escapeHtml(messageText)}
                </div>
                <div class="message-time">${this.getCurrentTime()}</div>
            `;

            messagesContainer.appendChild(messageElement);
            messageInput.value = '';

            // Scroll to bottom
            messagesContainer.scrollTop = messagesContainer.scrollHeight;

            // Simulate reply after 1 second
            setTimeout(() => {
                this.simulateReply();
            }, 1000);
        }
    }

    simulateReply() {
        const messagesContainer = document.querySelector('.messages');
        const replies = [
            "Thanks for your message!",
            "I'll get back to you soon.",
            "That's interesting, tell me more!",
            "I agree with you.",
            "Let me think about that..."
        ];

        const randomReply = replies[Math.floor(Math.random() * replies.length)];

        const messageElement = document.createElement('div');
        messageElement.className = 'message received';
        messageElement.innerHTML = `
            <div class="message-content">
                ${randomReply}
            </div>
            <div class="message-time">${this.getCurrentTime()}</div>
        `;

        messagesContainer.appendChild(messageElement);
        
        // Scroll to bottom
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    getCurrentTime() {
        const now = new Date();
        return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
}

// Initialize chat interface when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.chatApp = new ChatInterface();
});