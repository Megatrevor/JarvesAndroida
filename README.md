# Jarves Android

Primeira base funcional do assistente Jarves para Android.

## v0.2
- Ativação por voz usando a palavra Jarves.
- Reconhecimento em português do Brasil.
- Resposta por voz.
- Comandos para hora, WhatsApp, YouTube, Instagram, Facebook e configurações.
- Controle básico de volume.
- Serviço de microfone em primeiro plano.
- Estrutura para Tasker e Accessibility Service.
- Workflow para gerar APK debug.

## Próxima etapa
A evolução planejada é usar reconhecimento offline com Vosk e um modelo português. O Vosk Android 0.3.75 está disponível no Maven Central; o projeto oficial demonstra o uso do AAR. O modelo de voz precisa ser incluído no app.

O Android possui regras específicas para serviços de primeiro plano que usam microfone. Em Android 14 ou superior, é necessário declarar o tipo microphone e a permissão correspondente, e a inicialização precisa respeitar as regras de acesso ao microfone.