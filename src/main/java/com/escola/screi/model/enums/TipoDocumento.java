package com.escola.screi.model.enums;

public enum TipoDocumento {
    CPF_ALUNO("CPF do aluno", "cpfAlunoImagem"),
    CERTIDAO_NASCIMENTO("Certidão de nascimento", "certidaoNascimentoImagem"),
    CPF_RESPONSAVEL("CPF do responsável", "cpfResponsavelImagem"),
    COMPROVANTE_RESIDENCIA("Comprovante de residência", "comprovanteResidenciaImagem");

    private final String descricao;
    private final String campo;
    TipoDocumento(String descricao, String campo) { this.descricao=descricao; this.campo=campo; }
    public String getDescricao() { return descricao; }
    public String getCampo() { return campo; }
}
