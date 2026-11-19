# CampusHub

Aplicativo Android para descoberta, organização e acompanhamento de eventos universitários.

O projeto permite que alunos encontrem eventos e se inscrevam, enquanto organizadores criam, publicam, acompanham participantes e registram presenças por QR Code.

## Funcionalidades

### Aluno

- Criar conta, entrar, recuperar senha e editar o perfil.
- Consultar, pesquisar e filtrar eventos publicados.
- Ver detalhes do evento, localização no mapa, comentários e avaliações.
- Favoritar eventos e consultar inscrições em **Meus eventos**.
- Inscrever-se em eventos com vagas disponíveis.
- Entrar automaticamente na lista de espera quando não houver vaga.
- Exibir QR Code e código alternativo para uma inscrição confirmada.
- Avaliar eventos encerrados nos quais participou.

### Organizador

- Criar, editar e salvar eventos como rascunho.
- Publicar ou encerrar somente eventos sob sua responsabilidade.
- Informar título, descrição, categoria, data, local, endereço, capacidade e imagem de capa.
- Consultar os próprios eventos e o painel de acompanhamento.
- Ver inscritos confirmados, pessoas na espera, vagas ocupadas e check-ins realizados.
- Ler QR Code ou informar o código alternativo para registrar presença uma única vez.

### Regras de vagas e check-in

- As inscrições usam transações do Firestore para impedir que a capacidade seja ultrapassada.
- Ao cancelar uma inscrição confirmada, a próxima pessoa da lista de espera é promovida.
- Cada inscrição confirmada possui uma credencial única de QR Code.
- Um QR Code não pode ser validado mais de uma vez.

## Tecnologias

- Kotlin e Android SDK (mínimo: API 24)
- Material Components
- Firebase Authentication (e-mail e senha)
- Cloud Firestore e Firestore Security Rules
- Mapbox Maps SDK
- CameraX e ML Kit Barcode Scanning
- ZXing para geração de QR Code
- Arquitetura organizada por feature, repositories e ViewModels

## Como executar

1. Clone o repositório e abra-o no Android Studio.
2. No [Firebase Console](https://console.firebase.google.com/), crie ou selecione um projeto.
3. Registre um aplicativo Android com o pacote abaixo:

   ```text
   br.com.uri.campushub
   ```

4. Baixe o arquivo `google-services.json` e coloque-o em `app/google-services.json`.
5. Ative **Authentication > Sign-in method > E-mail/senha**.
6. Crie um banco **Cloud Firestore**.
7. Publique o conteúdo de [`firestore.rules`](firestore.rules) em **Firestore Database > Rules**.
8. Crie um token público do Mapbox e adicione-o ao arquivo `local.properties`:

   ```properties
   MAPBOX_ACCESS_TOKEN=SEU_TOKEN_PUBLICO
   ```

9. Sincronize o Gradle e execute o app em um dispositivo ou emulador Android com API 24 ou superior.

> `local.properties` e `app/google-services.json` estão no `.gitignore`. Não envie tokens ou credenciais privadas ao repositório.

## Perfis e permissões

Contas novas são criadas como `STUDENT`. Por segurança, o usuário não consegue mudar o próprio papel pelo app.

Para testar as funções de organizador, no Firebase Console altere manualmente o documento `users/{uid}` da conta de teste:

```text
role: ORGANIZER
```

As regras do Firestore garantem que somente o organizador responsável por um evento pode criá-lo, editá-lo, publicá-lo, encerrá-lo ou registrar check-ins.

## Fluxo de teste manual

1. Crie uma conta e altere seu `role` para `ORGANIZER` no Firestore.
2. Crie um evento com capacidade baixa, salve como rascunho e publique-o.
3. Crie uma segunda conta `STUDENT` e faça uma inscrição confirmada.
4. Inscreva uma terceira conta quando a capacidade estiver cheia e confirme o status **Em espera**.
5. Cancele a inscrição confirmada e confira a promoção da próxima pessoa da espera.
6. Na conta do aluno confirmado, abra o evento e toque em **Mostrar meu QR Code de check-in**.
7. Na conta organizadora, abra **Meus eventos > Acompanhamento** e use **Ler QR Code para check-in**.
8. Leia o QR Code ou informe o código alternativo de oito caracteres exibido para o aluno.
9. Tente validar novamente a mesma inscrição e confirme que ela é recusada.

## Testes automatizados

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

Para usar Firebase Emulator Suite durante os testes de integração e os fluxos Maestro:

```bash
npx firebase-tools emulators:start --only auth,firestore --project campushub-c6980
maestro test maestro
```

## Estrutura principal

```text
br.com.uri.campushub
├── core/firebase                 # configuração Firebase
└── feature
    ├── auth                      # login, cadastro e recuperação de senha
    ├── events                    # catálogo, editor, inscrições e check-in
    ├── home                      # início e atalhos por papel
    └── profile                   # perfil e papel do usuário
```

## Commits da atividade

Os commits relacionados a perfis, gerenciamento de eventos, capacidade, lista de espera, acompanhamento e check-in estão no [histórico do repositório](https://github.com/limarobs/CampusHub/commits/main/).
