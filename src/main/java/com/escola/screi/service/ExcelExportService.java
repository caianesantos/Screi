package com.escola.screi.service;

import com.escola.screi.repository.SolicitacaoMatriculaRepository;
import com.escola.screi.model.enums.StatusMatricula;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
@Service
public class ExcelExportService {
    private final SolicitacaoMatriculaRepository repository;
    public ExcelExportService(SolicitacaoMatriculaRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public byte[] exportar() throws IOException {
        try (var workbook=new XSSFWorkbook(); var output=new ByteArrayOutputStream()) {
            Sheet sheet=workbook.createSheet("Matrículas aprovadas");
            String[] headers={"Protocolo","Aluno","Nascimento","Certidão","CPF do aluno","Autorizados a buscar","Responsáveis","CPF dos responsáveis","Telefones","E-mails","Endereços","Contatos de emergência","Status","Submissão","Atualização"};
            Font font=workbook.createFont(); font.setBold(true);
            CellStyle style=workbook.createCellStyle(); style.setFont(font);
            Row header=sheet.createRow(0);
            for (int i=0;i<headers.length;i++) { header.createCell(i).setCellValue(headers[i]); header.getCell(i).setCellStyle(style); }
            int index=1;
            for (var s:repository.findByStatusOrderByDataSubmissaoDesc(StatusMatricula.APROVADO)) {
                var a=s.getAluno(); var rs=s.getResponsaveis();
                String[] values={s.getProtocolo(),a.getNomeCompleto(),a.getDataNascimento().toString(),a.getCertidaoNascimento(),a.getCpf(),a.getAutorizadosBuscar(),
                    rs.stream().map(r -> r.getNomeCompleto()+" ("+r.getParentesco()+")").collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getCpf()).collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getTelefone()).collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getEmail()).collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getLogradouro()+", "+r.getNumero()+", "+r.getBairro()+", "+r.getCidade()+"/"+r.getEstado()+" CEP "+r.getCep()).collect(Collectors.joining("; ")),
                    s.getContatosEmergencia().stream().map(c -> c.getNome()+" ("+c.getParentesco()+") "+c.getTelefone()).collect(Collectors.joining("; ")),
                    s.getStatus().name(),s.getDataSubmissao().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),s.getDataAtualizacao().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)};
                Row row=sheet.createRow(index++);
                for (int i=0;i<values.length;i++) row.createCell(i,CellType.STRING).setCellValue(values[i]);
            }
            sheet.createFreezePane(0,1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,index-1),0,headers.length-1));
            for (int i=0;i<headers.length;i++) sheet.setColumnWidth(i, i==0 ? 10000 : 6500);
            workbook.write(output); return output.toByteArray();
        }
    }
}
