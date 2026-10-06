# CampusHub

<p align="center">
  <strong>Eventos universitários, reunidos em um só lugar.</strong><br />
  Um aplicativo Android para alunos descobrirem eventos e gerenciarem suas inscrições.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white" alt="Android 24+" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Firebase-Authentication%20%7C%20Firestore-FFCA28?logo=firebase&logoColor=black" alt="Firebase" />
</p>

## Sobre o projeto

O **CampusHub** permite que alunos acessem a conta universitária, consultem eventos publicados e acompanhem as atividades em que estão inscritos.

## Funcionalidades

| Área | Recursos |
| --- | --- |
| Autenticação | Criar conta, login, logout e recuperação de senha por e-mail |
| Perfil | Visualizar e editar o nome do usuário |
| Eventos | Listar eventos disponíveis e consultar seus detalhes |
| Inscrições | Inscrever-se, cancelar inscrição e visualizar “Meus eventos” |

## Tecnologias

- Kotlin
- Android SDK e Material Components
- Firebase Authentication (e-mail e senha)
- Cloud Firestore
- Gradle com Kotlin DSL

## Organização do código

O projeto usa organização por **feature** e por responsabilidade:

```text
br.com.uri.campushub
├── core
│   └── firebase
│       └── FirebaseServices.kt
└── feature
    ├── auth
    │   └── ui
    ├── events
    │   ├── data
    │   ├── model
    │   └── ui
    ├── home
    │   └── ui
    └── profile
        ├── data
        └── ui
```

## Como executar

1. Clone o repositório e abra a pasta no Android Studio.
2. No [Firebase Console](https://console.firebase.google.com/), crie ou selecione o projeto Firebase.
3. Registre um app Android com o pacote:

   ```text
   br.com.uri.campushub
   ```

4. Baixe o arquivo `google-services.json` e coloque-o em:

   ```text
   app/google-services.json
   ```

5. No Firebase, ative **Authentication → E-mail/senha** e crie o banco no **Cloud Firestore**.
6. Sincronize o Gradle e execute o app em um emulador ou dispositivo Android com API 24 ou superior.

> O arquivo `google-services.json` é local e está ignorado pelo Git. Nunca o envie ao repositório.

## Regras do Firestore

Para a versão atual do app, publique estas regras no Cloud Firestore:

```js
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, create, update: if request.auth != null
          && request.auth.uid == userId;

      match /registrations/{eventId} {
        allow read, create, delete: if request.auth != null
            && request.auth.uid == userId;
      }
    }

    match /events/{eventId} {
      allow read: if request.auth != null;
    }
  }
}
```

## Estrutura dos eventos

Crie documentos na coleção `events` com os campos abaixo, todos como texto:

```text
title: Semana Acadêmica
description: Palestras e oficinas para os alunos.
date: 15/10/2026 às 19:00
location: Auditório Central
```

## Fluxo principal

```text
Login → Eventos disponíveis → Detalhes → Inscrever-se → Meus eventos
```

---

Desenvolvido para centralizar a experiência de eventos universitários.
