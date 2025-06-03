# Howly Velocity

Plugin core da rede Howly desenvolvido para Velocity.

> 🚧 Este projeto está em constante desenvolvimento e recebe atualizações diárias conforme as demandas da equipe.

---

## Funcionalidades

### Jogador
- Armazenamento de dados do jogador
- Histórico de login
- Sistema de tags personalizadas
- Sistema de medalhas
- Sistema de ignorar jogadores

### Chat
- Chat criado do zero
- Gerenciador de mensagens do chat
- Formatação com placeholders
- Sistema modular de placeholders dinâmicos
- Bloqueio de mensagens de jogadores ignorados
- Sistema de mensagens privadas
- Sistema de resposta rápida à última mensagem recebida
- Sistema de anúncios para toda a rede

### Punições
- Sistema de banimento (temporário e permanente)
- Sistema de mute (silenciamento de mensagens, temporário e permanente)
- Sistema de kick (expulsão do servidor)
- Sistema de eventos de punição
- API pública para controle e verificação de punições
- Execução assíncrona com banco de dados

### Backend
- Banco de dados H2 e MySQL
- Consultas assíncronas com HikariCP
- Arquitetura modular e desacoplada
- Listeners de login/logout para carregamento e salvamento de dados
- Gerenciamento de configurações via `ConfigManager.java`
- API pública acessível via `HowlyAPI.java`
- Utilitários de formatação de mensagens coloridas

### Utilitários e Administração
- Visualização de jogadores online na rede
- Localização de jogadores em servidores conectados
- Visualização das versões dos servidores
- Sistema de envio de jogadores entre servidores
- Sistema de consulta de informações detalhadas de jogadores

![image](https://github.com/user-attachments/assets/c46cd147-bb29-438c-9d3f-69fb110362ff)
