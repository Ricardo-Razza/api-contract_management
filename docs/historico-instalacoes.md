# Histórico de instalações

Contrato: `GET /impressoras/{id}/instalacoes` e `GET /locais-instalacao/{id}/instalacoes` retornam uma lista de passagens, da mais recente para a mais antiga (data e ID decrescentes), incluindo instalações atuais e encerradas. ID inexistente retorna 404; histórico vazio retorna `[]`.

Cada passagem contém `id`, `impressoraId`, `itemPedido`, `numeroSerie`, `fabricante`, `modelo`, `localInstalacaoId`, `localInstalacao`, `secretariaId`, `secretariaSigla`, `dataInstalacao`, `dataRetirada`, `status` e `motivoRetirada`. Datas usam YYYY-MM-DD; retirada e motivo podem ser nulos.

Cadastro, edição e remanejamento exigem um `localInstalacaoId` ativo e pertencente à secretaria informada. Alterar a localização deve usar remanejamento, preservando a instalação anterior; a edição cadastral rejeita essa alteração.

Na inicialização, a rotina existente de locais acrescenta `local_instalacao_id` à tabela de instalações se necessário e associa registros legados por nome e secretaria. A associação por ID preserva o histórico mesmo após renomear um local. Os dados históricos que nunca foram registrados não podem ser reconstruídos.
