# 🌐 Howly Velocity

**Plugin core da rede Howly desenvolvido para Velocity.**  
> 🚧 Projeto em constante desenvolvimento, com atualizações diárias conforme as demandas da equipe.

---

## 👤 Funcionalidades – Jogador

- Armazenamento e carregamento assíncrono de dados
- Histórico completo de login com data e hora
- Visualização de tempo online
- Sistema de identificação única de usuários
- Tags personalizadas e sistema de medalhas
- Sistema para ignorar mensagens de jogadores específicos

---

## 💬 Funcionalidades – Chat

- Chat criado do zero, totalmente customizável
- Gerenciador completo de mensagens com placeholders dinâmicos
- Sistema de mensagens privadas e resposta rápida (/r)
- Suporte a múltiplos chats: StaffChat, YoutuberChat, BunkerChat, etc.
- Sistema de grupos integrado ao chat
- Anúncios em toda a rede

---

## 🚫 Funcionalidades – Punições

- Banimentos temporários e permanentes
- Silenciamentos (mutes) e expulsões (kicks)
- Registro e notificação de eventos de punição
- API pública para controle e verificação
- Execução assíncrona integrada ao banco de dados

---

## 🧠 Funcionalidades – Backend

- Suporte aos bancos H2 e MySQL
- Consultas otimizadas com HikariCP
- Arquitetura modular, organizada por serviços e escopos
- API pública via `HowlyAPI.java`
- Gerenciamento centralizado de configurações (`ConfigManager.java`)
- Utilitários para formatação e mensagens coloridas
- Listeners de login/logout para carregamento e salvamento de dados

---

## 🛠️ Funcionalidades – Utilitários e Administração

- Visualização de jogadores online e tempo de conexão
- Consulta de informações detalhadas (UUID, IP, tags, histórico)
- Envio e movimentação entre servidores
- Monitoramento de versões dos servidores conectados
- Sistema de manutenção
- Sistema de MOTD
