
let websocket = null
let isChatOpen = false

function toggleChat() {
    const chatWindow = document.getElementById('chatWindow');
    isChatOpen = !isChatOpen;

    if (isChatOpen) {
        chatWindow.style.display = 'flex';
        connectWebSocket();
        document.getElementById('messageInput').focus();
    } else {
        chatWindow.style.display = 'none';
        disconnectWebSocket();
    }
}

function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = protocol + '//' + window.location.host + '/api/users/support';

    websocket = new WebSocket(wsUrl);

    websocket.onopen = function(event) {
        console.log('Websocket connection opened');
    };

    websocket.onmessage = function(event) {
        try {
            const message = JSON.parse(event.data);
            addMessage(message.reply, 'bot');
        } catch (e) {
            console.log('Error parsing message:', e);
            addMessage('Error processing message', 'bot');
        }
    };

    websocket.onclose = function(event) {
        console.log('Websocket connection closed');
    };

    websocket.onerror = function(error) {
        console.log('Websocket error:', error);
        addMessage('Connection error. Please try again.', 'bot')
    };
}

function disconnectWebSocket() {
    if (websocket) {
        websocket.close();
        websocket = null;
    }
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();

    if (message && websocket && websocket.readyState === WebSocket.OPEN) {
        const messageData = {
            message: message
        };

        websocket.send(JSON.stringify(messageData));
        addMessage(message, 'user');
        input.value = '';
    }
}

function handleKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
        event.preventDefault();
    }
}

function addMessage(text, sender) {
    const messageContainer = document.getElementById('messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;
    messageDiv.textContent = text;

    messageContainer.appendChild(messageDiv);
    messageContainer.scrollTop = messageContainer.scrollHeight;
}

window.addEventListener('beforeunload', function() {
    disconnectWebSocket();
});

document.addEventListener('click', function(event) {
    const chatWindow = document.getElementById('chatWindow');
    const chatButton = document.querySelector('.chat-button');

    if (
        isChatOpen &&
        !chatWindow.contains(event.target) &&
        !chatButton.contains(event.target)
    ) {
        toggleChat();
    }
});
