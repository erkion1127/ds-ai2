class ChatApp {
    constructor() {
        this.sessionId = null;
        this.baseUrl = 'http://localhost:8080/api/v1';
        this.init();
    }
    
    init() {
        this.setupElements();
        this.setupEventListeners();
        this.createNewSession();
    }
    
    setupElements() {
        this.chatMessages = document.getElementById('chatMessages');
        this.messageInput = document.getElementById('messageInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.newChatBtn = document.getElementById('newChatBtn');
        this.ragToggle = document.getElementById('ragToggle');
        this.directMode = document.getElementById('directMode');
        this.modelSelect = document.getElementById('modelSelect');
        this.sessionIdDisplay = document.getElementById('sessionId');
        this.charCount = document.getElementById('charCount');
    }
    
    setupEventListeners() {
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        this.newChatBtn.addEventListener('click', () => this.createNewSession());
        
        this.directMode.addEventListener('change', () => {
            if (this.directMode.checked) {
                this.ragToggle.checked = false;
                this.ragToggle.disabled = true;
                this.modelSelect.style.display = 'inline-block';
                this.loadAvailableModels();
            } else {
                this.ragToggle.disabled = false;
                this.modelSelect.style.display = 'none';
            }
        });
        
        this.ragToggle.addEventListener('change', () => {
            if (this.ragToggle.checked && this.directMode.checked) {
                this.directMode.checked = false;
                this.modelSelect.style.display = 'none';
            }
        });
        
        this.messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
        
        this.messageInput.addEventListener('input', () => {
            this.updateCharCount();
            this.adjustTextareaHeight();
            this.sendBtn.disabled = this.messageInput.value.trim().length === 0;
        });
    }
    
    updateCharCount() {
        const length = this.messageInput.value.length;
        this.charCount.textContent = `${length} / 1000`;
        if (length > 1000) {
            this.charCount.style.color = '#c00';
        } else {
            this.charCount.style.color = '#999';
        }
    }
    
    adjustTextareaHeight() {
        this.messageInput.style.height = 'auto';
        this.messageInput.style.height = Math.min(this.messageInput.scrollHeight, 120) + 'px';
    }
    
    async createNewSession() {
        try {
            const response = await fetch(`${this.baseUrl}/chat/new`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            const data = await response.json();
            if (data.success) {
                this.sessionId = data.data;
                this.sessionIdDisplay.textContent = this.sessionId.substring(0, 8) + '...';
                this.clearChat();
                this.showWelcomeMessage();
            }
        } catch (error) {
            console.error('Error creating new session:', error);
            this.showError('세션 생성 실패');
        }
    }
    
    clearChat() {
        this.chatMessages.innerHTML = '';
    }
    
    showWelcomeMessage() {
        const welcomeDiv = document.createElement('div');
        welcomeDiv.className = 'welcome-message';
        welcomeDiv.innerHTML = `
            <p>안녕하세요! AI 어시스턴트입니다.</p>
            <p>무엇을 도와드릴까요?</p>
        `;
        this.chatMessages.appendChild(welcomeDiv);
    }
    
    async sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message || message.length > 1000) return;
        
        // Clear welcome message if exists
        const welcomeMsg = this.chatMessages.querySelector('.welcome-message');
        if (welcomeMsg) {
            welcomeMsg.remove();
        }
        
        // Add user message
        this.addMessage(message, 'user');
        
        // Clear input
        this.messageInput.value = '';
        this.updateCharCount();
        this.adjustTextareaHeight();
        this.sendBtn.disabled = true;
        
        // Show typing indicator
        const typingIndicator = this.showTypingIndicator();
        
        try {
            let response;
            
            if (this.directMode.checked) {
                // Direct Ollama mode
                response = await fetch(`${this.baseUrl}/direct-chat`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        message: message,
                        model: this.modelSelect.value || null
                    })
                });
            } else {
                // Normal chat mode (with or without RAG)
                response = await fetch(`${this.baseUrl}/chat`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        sessionId: this.sessionId,
                        message: message,
                        useRag: this.ragToggle.checked
                    })
                });
            }
            
            const data = await response.json();
            
            // Remove typing indicator
            typingIndicator.remove();
            
            if (data.success) {
                if (this.directMode.checked) {
                    const modelInfo = data.data.model ? `[${data.data.model}]` : '';
                    this.addMessage(data.data.response, 'assistant', null, modelInfo);
                } else {
                    this.addMessage(data.data.response, 'assistant', data.data.sources);
                }
            } else {
                this.showError(data.message || '응답 처리 실패');
            }
        } catch (error) {
            console.error('Error sending message:', error);
            typingIndicator.remove();
            this.showError('메시지 전송 실패');
        }
    }
    
    addMessage(content, type, sources = null, modelInfo = null) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        
        // Add model info for direct mode
        if (modelInfo && type === 'assistant') {
            const modelDiv = document.createElement('div');
            modelDiv.className = 'model-info';
            modelDiv.textContent = modelInfo;
            modelDiv.style.fontSize = '12px';
            modelDiv.style.color = '#999';
            modelDiv.style.marginBottom = '5px';
            contentDiv.appendChild(modelDiv);
        }
        
        // Convert newlines to <br> for better formatting
        const formattedContent = content.replace(/\n/g, '<br>');
        const textDiv = document.createElement('div');
        textDiv.innerHTML = formattedContent;
        contentDiv.appendChild(textDiv);
        
        // Add timestamp
        const timeDiv = document.createElement('div');
        timeDiv.className = 'message-time';
        timeDiv.textContent = new Date().toLocaleTimeString('ko-KR', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
        contentDiv.appendChild(timeDiv);
        
        // Add sources if available (for assistant messages with RAG)
        if (sources && sources.length > 0) {
            const sourcesDiv = document.createElement('div');
            sourcesDiv.className = 'sources';
            sourcesDiv.innerHTML = '<div class="sources-title">📚 참고 자료:</div>';
            sources.forEach(source => {
                const sourceItem = document.createElement('div');
                sourceItem.className = 'source-item';
                sourceItem.textContent = `• ${source}`;
                sourcesDiv.appendChild(sourceItem);
            });
            contentDiv.appendChild(sourcesDiv);
        }
        
        messageDiv.appendChild(contentDiv);
        this.chatMessages.appendChild(messageDiv);
        
        // Scroll to bottom
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }
    
    showTypingIndicator() {
        const typingDiv = document.createElement('div');
        typingDiv.className = 'message assistant';
        typingDiv.innerHTML = `
            <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
            </div>
        `;
        this.chatMessages.appendChild(typingDiv);
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
        return typingDiv;
    }
    
    showError(message) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.textContent = `❌ ${message}`;
        this.chatMessages.appendChild(errorDiv);
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
        
        // Remove error message after 5 seconds
        setTimeout(() => {
            errorDiv.remove();
        }, 5000);
    }
    
    async loadAvailableModels() {
        try {
            const response = await fetch(`${this.baseUrl}/direct-chat/models`);
            const data = await response.json();
            
            if (data.success && data.data.models) {
                // Clear existing options
                this.modelSelect.innerHTML = '<option value="">기본 모델</option>';
                
                // Add available models
                data.data.models.forEach(model => {
                    const option = document.createElement('option');
                    option.value = model.name;
                    option.textContent = model.name;
                    this.modelSelect.appendChild(option);
                });
                
                // Set default model if specified
                if (data.data.defaultModel) {
                    this.modelSelect.value = data.data.defaultModel;
                }
            }
        } catch (error) {
            console.error('Error loading models:', error);
        }
    }
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});