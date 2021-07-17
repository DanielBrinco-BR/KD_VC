<h2>Protótipo de Rastreador Android com comunicação via API REST.</h2>

O "KdVc" é um protótipo de aplicativo rastreador de celulares Android, coleta dados de localização e envia para uma API REST na "nuvem", também recebe dados de outros celulares que possuirem o mesmo aplicativo instalado
e que tenham os números a serem rastreados devidamente cadastrados. Por segurança os dados enviados são criptografados pelo aplicativo e permanecem na "nuvem" somente até o celular monitor realizar a coleta dos mesmos. 

Arquitetura do aplicativo:

![alt text](https://github.com/DanielBrinco-BR/KD_VC/blob/main/Android_KdVc_Diagram.png?raw=true)

É recomendado testar o aplicativo cadastrando o próprio número e observar como o rastreamento é feito.

Após instalar o aplicativo em um celular Android execute as etapas abaixo:

```
- Escolher o seu número de telefone no popup que será exibido na primeira execução do aplicativo;
- Aceitar todas as permissões solicitadas;
- Tocar no botão flutuante na parte inferior da tela (+) e cadastrar o(s) número(s) do(s) celular(es) a ser(em) monitorado(s);
- Caso permita também ser rastreado, clicar no ícone de localização na barra de menu e ativar o rastreador do aplicativo;
- Executar o mesmo procedimento no celular a ser rastreado (com o consentimento do proprietário);
```

Pontos importantes:

* O aplicativo "KdVc" é um protótipo e o desenvolvedor não se responsabiliza por problemas que eventualmente possam ocorrer nem garante a integridade dos dados enviados ou recebidos.
* Ao instalar o aplicativo "KdVc" o usuário se responsabiliza a respeitar a legislação vigente relativa a crimes cibernéticos (Lei 12.737/2012) bem como a  Lei Geral de Proteção de Dados Pessoais (LGPD), Lei nº 13.709, de 14 de agosto de 2018. Qualquer violação legal é de inteira responsabilidade do usuário.
