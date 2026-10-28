# CampusHub

<p align="center">
  <strong>Eventos universitários, reunidos em um só lugar.</strong><br />
  Aplicativo Android para alunos encontrarem, acompanharem e interagirem com eventos da universidade.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white" alt="Android 24+" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Firebase-Authentication%20%7C%20Firestore-FFCA28?logo=firebase&logoColor=black" alt="Firebase" />
  <img src="https://img.shields.io/badge/Mapbox-Maps-000000?logo=mapbox&logoColor=white" alt="Mapbox" />
</p>

## Sobre o projeto

O **CampusHub** permite que alunos criem uma conta, consultem eventos universitários e acompanhem suas inscrições. Nos detalhes de cada evento, o aluno pode ver o local no mapa, favoritar, comentar, avaliar e publicar mídias relacionadas ao evento.

## Funcionalidades

| Área | Recursos |
| --- | --- |
| Autenticação | Criar conta, login, logout e recuperação de senha por e-mail |
| Perfil | Visualizar e editar o nome do usuário |
| Eventos | Listar, pesquisar, filtrar por categoria/situação e consultar detalhes |
| Localização | Exibir endereço e ponto do evento no mapa |
| Inscrições | Inscrever-se, cancelar inscrição e visualizar “Meus eventos” |
| Favoritos | Favoritar, desfavoritar e consultar “Meus favoritos” |
| Comentários | Publicar, editar e excluir apenas os próprios comentários |
| Avaliações | Alunos inscritos avaliam eventos encerrados com nota de 1 a 5 |
| Fotos | Tirar foto ou selecionar da galeria; arquivos ficam locais no dispositivo |
| Áudios | Gravar, reproduzir com progresso e excluir o próprio áudio local |
| Vídeos | Gravar ou selecionar vídeo, assistir e excluir o próprio vídeo local |

Fotos, áudios e vídeos apresentam autor e data. Essas publicações são independentes de inscrição, comentários e avaliações.

## Tecnologias

- Kotlin
- Android SDK e Material Components
- Firebase Authentication (e-mail e senha)
- Cloud Firestore
- Mapbox Maps SDK
- Arquitetura por feature, repositories e ViewModels
- Gradle com Kotlin DSL

## Organização do código

```text
br.com.uri.campushub
├── core
│   └── firebase
├── feature
│   ├── auth
│   │   └── ui
│   ├── events
│   │   ├── data
│   │   ├── model
│   │   ├── ui
│   │   └── viewmodel
│   ├── home
│   │   └── ui
│   └── profile
│       ├── data
│       └── ui
```

## Como executar

1. Clone o repositório e abra a pasta no Android Studio.
2. No [Firebase Console](https://console.firebase.google.com/), crie ou selecione um projeto Firebase.
3. Registre o app Android com o pacote:

   ```text
   br.com.uri.campushub
   ```

4. Baixe `google-services.json` e coloque-o em `app/google-services.json`.
5. Ative **Authentication → E-mail/senha** e crie o banco **Cloud Firestore**.
6. Crie um token público do Mapbox e adicione-o ao arquivo `local.properties`:

   ```properties
   MAPBOX_ACCESS_TOKEN=SEU_TOKEN_PUBLICO
   ```

7. Sincronize o Gradle e execute em um dispositivo ou emulador com API 24 ou superior.

> `local.properties` e `app/google-services.json` são arquivos locais, ignorados pelo Git. Nunca envie credenciais privadas ao repositório.

## Estrutura dos eventos no Firestore

Crie documentos na coleção `events`. Campos principais:

```text
title: Semana Acadêmica                 // string
description: Palestras e oficinas.      // string
date: 15/10/2026 às 19:00               // string
location: Auditório Central             // string
address: Rua Exemplo, 100               // string
latitude: -27.633                       // double
longitude: -52.275                      // double
category: Acadêmico                     // string
startsAt: timestamp                     // timestamp
endsAt: timestamp                       // timestamp
```

## Fluxo principal

```text
Login → Eventos → Detalhes → Inscrição/Favorito/Interações → Meus eventos e Meus favoritos
```

---

Desenvolvido para centralizar a experiência de eventos universitários.
