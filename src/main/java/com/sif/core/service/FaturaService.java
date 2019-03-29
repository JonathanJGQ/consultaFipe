package com.sif.core.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sif.core.utils.ConfiguracaoHelper;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.core.utils.TipoPerfil;
import com.sif.model.Averbadora;
import com.sif.model.Bilhetagem;
import com.sif.model.Configuracao;
import com.sif.model.Consignataria;
import com.sif.model.Fatura;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Orgao;
import com.sif.model.Usuario;
import com.sif.model.custom.BilhetagemCustomDTO;
import com.sif.model.custom.FaturaCustomDTO;
import com.sif.model.custom.FaturaListDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.FaturaRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.specification.FaturaSpecification;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;


@Service
public class FaturaService {

	public static final String STATUS_FATURA_ABERTA = "ABERTA";
	public static final String STATUS_FATURA_PAGA = "PAGA";
	public static final String STATUS_FATURA_VENCIDA = "VENCIDA";
	public static final String STATUS_FATURA_BAIXADA = "BAIXADA";
	@Autowired
	FaturaRepository repository;
	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	@Autowired
	JavaMailSender mailSender;
	@Autowired
	SimpleMailMessage simpleMailMessage;
	@Autowired
	BilhetagemService bilhetagemService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	LogAcaoService logAcaoService;
	@Autowired
	UsuarioService usuarioService;
	@Autowired
	Funcoes funcoes;
//	@Autowired
//	LogService logService;
	@Autowired
	SifLogUtil logUtil;
	@Autowired
	LogRepository logRepository;
	
	@Autowired
	AverbadoraService averbadoraService;
	
	@Autowired
	ConsignatariaService consignatariaService;
	
	@Autowired
	OrgaoService orgaoService;
	
	public List<Fatura> pesquisar(Fatura filter) {
		FaturaSpecification spec = new FaturaSpecification();
		return repository.findAll(Specification
				.where(spec.dataFechamentoEq(filter.getDataFechamento()))
				.and(spec.emailEnviadoEq(filter.getEmailEnviado()))
				);
	}
	
	@Scheduled(cron = "${fatura.cron.expression}")
	public void enviarEmailNovaFatura() {
		
		Fatura filter = new Fatura();
		filter.setStatus(STATUS_FATURA_ABERTA);
		filter.setEmailEnviado(false);
		filter.setStatus(STATUS_FATURA_ABERTA);
		List<Fatura> faturasAberto = pesquisar(filter);
		
		if(faturasAberto == null || faturasAberto.isEmpty()) {
			System.out.println("NENHUMA FATURA PENDENTE PARA ENVIO DE EMAIL");
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy");
		faturasAberto.stream()
		.filter(f -> f.getAverbadora().getEmail() != null 
				&& !f.getAverbadora().getEmail().isEmpty())
		.forEach(e -> {
			enviarEmail(e.getAverbadora().getEmail(), montarMensagemNovaFatura(e), "Fatura " + sdf.format(e.getDataVencimento()), e.getId());
			marcarFaturaMailEnviado(e.getId());
		});
		
	}
	
	public ResponseEntity<FaturaCustomDTO> findById(Long id){
		
		Fatura fatura = repository.findById(id).orElse(null);
		
		if(fatura != null) {
			FaturaCustomDTO faturaCustomDTO = faturaToDTO(fatura);
			
			return Optional
					.ofNullable(faturaCustomDTO)
					.map(usuario -> ResponseEntity.ok().body(usuario))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		
		return null;
	}
	
	public Page<FaturaListDTO> getAll(Pageable pageable, Fatura fatura){
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		FaturaSpecification faturaSpec = new FaturaSpecification();
		
		List<Fatura> faturas = new ArrayList<Fatura>();
		
		Long idAverbadora = null;
		if(fatura.getAverbadora() != null) {
			idAverbadora = fatura.getAverbadora().getId();
		}
		
//		if(fatura.getDataFechamentoFinal() != null) {
//			Calendar calLe = Calendar.getInstance();
//			calLe.setTime(fatura.getDataFechamentoFinal());
//			calLe.add(Calendar.DAY_OF_MONTH, 1);
//			fatura.setDataFechamentoFinal(calLe.getTime());
//		}
		
		if(fatura.getDataVencimentoFinal() != null) {
			Calendar calLe = Calendar.getInstance();
			calLe.setTime(fatura.getDataVencimentoFinal());
			calLe.add(Calendar.DAY_OF_MONTH, 1);
			fatura.setDataVencimentoFinal(calLe.getTime());
		}
		
		if(usuarioLogado.getPerfil().getId() == TipoPerfil.SUPREMO
			|| usuarioLogado.getPerfil().getId() == TipoPerfil.ADMINISTRADOR) {
			
			
			
			faturas = repository.findAll(Specification.where(
						faturaSpec.codigoFaturaEq(fatura.getCodigoFatura()))
					.and(faturaSpec.dataFechamentoEq(fatura.getDataBaixa()))
					.and(faturaSpec.dataBaixaEq(fatura.getDataBaixa()))
					.and(faturaSpec.nomeAverbadoraLike(fatura.getNomeAverbadora()))
					.and(faturaSpec.statusEq(fatura.getStatus()))
					.and(faturaSpec.dataFechamentoGe(fatura.getDataFechamentoInicial()))
					.and(faturaSpec.dataFechamentoLe(fatura.getDataFechamentoFinal()))
					.and(faturaSpec.dataVencimentoGe(fatura.getDataVencimentoInicial()))
					.and(faturaSpec.dataVencimentoLe(fatura.getDataVencimentoFinal()))
					.and(faturaSpec.orderByIdDESC())
					);
			
		} else if(usuarioLogado.getPerfil().getId() == TipoPerfil.ORGAO) {
			
			Orgao orgao = orgaoService.findOrgaoById(usuarioLogado.getEntidade());
			List<Averbadora> averbadoras = averbadoraService.findAverbadoraByOrgao(orgao.getId());
			List<Long> ids = new ArrayList<Long>();
			
			for(Averbadora averb : averbadoras) {
				ids.add(averb.getId());
			}
			
			faturas = repository.findAll(Specification.where(
					faturaSpec.codigoFaturaEq(fatura.getCodigoFatura()))
				.and(faturaSpec.dataFechamentoEq(fatura.getDataBaixa()))
				.and(faturaSpec.dataBaixaEq(fatura.getDataBaixa()))
				.and(faturaSpec.idAverbadoraEq(idAverbadora))
				.and(faturaSpec.idAverbadoraIn(ids))
				.and(faturaSpec.dataFechamentoGe(fatura.getDataFechamentoInicial()))
				.and(faturaSpec.dataFechamentoLe(fatura.getDataFechamentoFinal()))
				.and(faturaSpec.dataVencimentoGe(fatura.getDataVencimentoInicial()))
				.and(faturaSpec.dataVencimentoLe(fatura.getDataVencimentoFinal()))
				.and(faturaSpec.statusEq(fatura.getStatus()))
				.and(faturaSpec.orderByIdDESC())
				);
			
		} else if (usuarioLogado.getPerfil().getId() == TipoPerfil.AVERBADORA) {
			
			Averbadora averbadora 
				= averbadoraService.findAverbadoraById(usuarioLogado.getEntidade());
			
			faturas = repository.findAll(Specification.where(
					faturaSpec.codigoFaturaEq(fatura.getCodigoFatura()))
				.and(faturaSpec.dataFechamentoEq(fatura.getDataBaixa()))
				.and(faturaSpec.dataBaixaEq(fatura.getDataBaixa()))
				.and(faturaSpec.idAverbadoraEq(averbadora.getId()))
				.and(faturaSpec.statusEq(fatura.getStatus()))
				.and(faturaSpec.dataFechamentoGe(fatura.getDataFechamentoInicial()))
				.and(faturaSpec.dataFechamentoLe(fatura.getDataFechamentoFinal()))
				.and(faturaSpec.dataVencimentoGe(fatura.getDataVencimentoInicial()))
				.and(faturaSpec.dataVencimentoLe(fatura.getDataVencimentoFinal()))
				.and(faturaSpec.orderByIdDESC())
				);
			
		} else if(usuarioLogado.getPerfil().getId() == TipoPerfil.CONSIGNATARIA) {
			
			Consignataria consignataria = consignatariaService.findConsignatariaById(usuarioLogado.getEntidade());
			
			if(consignataria.getAverbadora() != null) {
				idAverbadora = consignataria.getAverbadora().getId();
			}
			
			faturas = repository.findAll(Specification.where(
					faturaSpec.codigoFaturaEq(fatura.getCodigoFatura()))
				.and(faturaSpec.dataFechamentoEq(fatura.getDataBaixa()))
				.and(faturaSpec.dataBaixaEq(fatura.getDataBaixa()))
				.and(faturaSpec.idAverbadoraEq(idAverbadora))
				.and(faturaSpec.statusEq(fatura.getStatus()))
				.and(faturaSpec.dataFechamentoGe(fatura.getDataFechamentoInicial()))
				.and(faturaSpec.dataFechamentoLe(fatura.getDataFechamentoFinal()))
				.and(faturaSpec.dataVencimentoGe(fatura.getDataVencimentoInicial()))
				.and(faturaSpec.dataVencimentoLe(fatura.getDataVencimentoFinal()))
				.and(faturaSpec.orderByIdDESC())
				);
		}
		
		List<FaturaListDTO> listaRetorno = new ArrayList<FaturaListDTO>();
		
		for(Fatura faturaToDTO : faturas) {
			listaRetorno.add(faturaToListDTO(faturaToDTO));
		}
		
		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return new PageImpl<FaturaListDTO>(listaRetorno.subList(start, end), pageable, listaRetorno.size());
	}
	
	public FaturaListDTO faturaToListDTO(Fatura fatura) {
		FaturaListDTO faturaListDTO = new FaturaListDTO();
		
		Averbadora averbadora = fatura.getAverbadora();
		
		faturaListDTO.setId(fatura.getId());
		faturaListDTO.setDataFechamento(fatura.getDataFechamento());
		faturaListDTO.setCodigoFatura(fatura.getCodigoFatura());
		faturaListDTO.setDataVencimento(fatura.getDataVencimento());
		faturaListDTO.setDataBaixa(fatura.getDataBaixa());
		
		if(averbadora != null) {
			faturaListDTO.setNomeAverbadora(averbadora.getNome());
		}
		
		faturaListDTO.setStatus(fatura.getStatus());
		
		if(fatura.getValorTotal() != null) {
			faturaListDTO.setValorTotal(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorTotal()));
		}
		
		faturaListDTO.setCodigoFatura(fatura.getId());
		
		return faturaListDTO;
	}
	
	public FaturaCustomDTO faturaToDTO(Fatura fatura) {
		
		FaturaCustomDTO faturaCustomDTO = new FaturaCustomDTO();

		faturaCustomDTO.setId(fatura.getId());
		
		if(fatura.getAverbadora() != null) {
			faturaCustomDTO.setAverbadora(fatura.getAverbadora().getId());
			faturaCustomDTO.setNomeAverbadora(fatura.getAverbadora().getNome());
		}
		
		faturaCustomDTO.setQtdNovosContratos(fatura.getQtdNovosContratos());
		
		faturaCustomDTO.setValorNovosContratos(fatura.getValorNovosContratos());
		
		faturaCustomDTO.setValorNovosContratosCurrency(
			NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorNovosContratos())
		);
		
		faturaCustomDTO.setQtdEdicaoContratos(fatura.getQtdEdicaoContratos());
		
		faturaCustomDTO.setValorEdicao(fatura.getValorEdicao());
		
		faturaCustomDTO.setValorEdicaoCurrency(
			NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorEdicao())
		);
		
		faturaCustomDTO.setQtdBaixaContratos(fatura.getQtdBaixaContratos());
		
		faturaCustomDTO.setValorBaixaContratos(fatura.getValorBaixaContratos());
		
		faturaCustomDTO.setValorBaixaContratosCurrency(
			NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorBaixaContratos())
		);
		
		faturaCustomDTO.setQtdCancelamentoContratos(fatura.getQtdCancelamentoContratos());
		
		faturaCustomDTO.setValorCancelamentoContratos(fatura.getValorCancelamentoContratos());
		
		faturaCustomDTO.setValorCancelamentoContratosCurrency(
			NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorCancelamentoContratos())
		);
		
		faturaCustomDTO.setQtdConsultaMargem(fatura.getQtdCancelamentoContratos());
		
		faturaCustomDTO.setValorConsultaMargem(fatura.getValorConsultaMargem());
		
		faturaCustomDTO.setValorConsultaMargemCurrency(
			NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorConsultaMargem())
		);
		
		faturaCustomDTO.setDataFechamento(fatura.getDataFechamento());
		
		faturaCustomDTO.setDataBaixa(fatura.getDataBaixa());
		
		faturaCustomDTO.setDataVencimento(fatura.getDataVencimento());
		
		faturaCustomDTO.setMotivoBaixa(fatura.getMotivoBaixa());
		
		faturaCustomDTO.setStatus(fatura.getStatus());
		
		faturaCustomDTO.setEmailEnviado(fatura.getEmailEnviado());
		
		faturaCustomDTO.setValorTotal(fatura.getValorTotal());
		
		faturaCustomDTO.setValorTotalCurrency(
			NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorTotal())
		);
		
		faturaCustomDTO.setValorDaOperacao(fatura.getValorDaOperacao());
		
		faturaCustomDTO.setValorTotalCurrency(
			NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(fatura.getValorTotal())
		);
		
		faturaCustomDTO.setCodigoFatura(fatura.getCodigoFatura());
		
		Bilhetagem bilhetagem = new Bilhetagem();
		bilhetagem.setFatura(fatura);
		List<Bilhetagem> bilhetagens = bilhetagemService.pesquisarForFatura(bilhetagem);
		
		List<BilhetagemCustomDTO> customBilhetagens = new ArrayList<BilhetagemCustomDTO>();
		
		for(Bilhetagem bilhete : bilhetagens) {
			customBilhetagens.add(bilhetagemToDTO(bilhete));
		}
		
		faturaCustomDTO.setBilhetagem(customBilhetagens);
		
		return faturaCustomDTO;
		
	}
	
	private BilhetagemCustomDTO bilhetagemToDTO(Bilhetagem bilhetagem) {
		BilhetagemCustomDTO bilhetCustom = new BilhetagemCustomDTO();
		
		bilhetCustom.setNumContrato(bilhetagem.getContrato().getNumeroContrato());
		bilhetCustom.setDataTransacao(bilhetagem.getData());
		bilhetCustom.setDescricaoOperacao(bilhetagem.getStatusTransacao().getDescricao());
		bilhetCustom.setFuncionario(bilhetagem.getUsuario().getNome());
		
		return bilhetCustom;
	}
	
	private String montarMensagemNovaFatura(Fatura fatura) {
		
		String body = "Sua fatura do mês de " + getMesStr(fatura.getDataVencimento()) +
				" fechou.\n\n"
				+ "A fatura segue anexo neste email.\n\n"
				+ " Se você já efetuou o pagamento da sua fatura, é só esperar. Ele será reconhecido e o seu limite liberado entre 3 e 5 dias úteis.\n\n\n\n"
				+ "Att,"
				+ "Sistema Auttis-RC.";
		return body;
	}
	
	@SuppressWarnings("deprecation")
	public String getMesStr(Date dt) {
		int month = dt.getMonth();
		switch(month) {
		case 0:
			return "JANEIRO";
		case 1:
			return "FEVEREIRO";
		case 2:
			return "MARÇO";
		case 3:
			return "ABRIL";
		case 4:
			return "MAIO";
		case 5:
			return "JUNHO";
		case 6:
			return "JULHO";
		case 7:
			return "AGOSTO";
		case 8:
			return "SETEMBRO";
		case 9:
			return "OUTUBRO";
		case 10:
			return "NOVEMBRO";
		case 11:
			return "DEZEMBRO";
		default:
			return "";
		}
	}
	
	private void marcarFaturaMailEnviado(Long idFatura) {
		Fatura fatura = get(idFatura);
		if(fatura == null)
			return;
		fatura.setEmailEnviado(true);
		repository.save(fatura);
	}
	
	public Fatura get(Long idFatura) {
		repository.flush();
		return repository.findById(idFatura).orElse(null);
	}
	
	public ResponseEntity<String> baixarFatura(Long id, String motivo){
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		String message = "";
		String title = "";
		
		if(usuarioLogado.getPerfil().getId() != TipoPerfil.SUPREMO
			&& usuarioLogado.getPerfil().getId() != TipoPerfil.ADMINISTRADOR) {
			
			message = "Você não pode baixar esta fatura";
			title = "Erro";
			
			jsonObject.put("title", title);
			jsonObject.put("message", message);
			
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
			
		}
		
		Fatura fatura = get(id);
		if(fatura == null) {
			message = "Esta fatura não existe";
			title = "Erro";
			
			jsonObject.put("title", title);
			jsonObject.put("message", message);
			
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(404).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		if(fatura.getStatus().equals(STATUS_FATURA_BAIXADA)) {
			message = "Esta fatura já está baixada";
			title = "Erro";
			
			jsonObject.put("title", title);
			jsonObject.put("message", message);
			
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
//		fatura.setStatus(STATUS_FATURA_BAIXADA);
		
		Fatura faturaNew = new Fatura();
		BeanUtils.copyProperties(fatura, faturaNew);
		faturaNew.setStatus(STATUS_FATURA_BAIXADA);
		faturaNew.setMotivoBaixa(motivo);
		faturaNew.setDataBaixa(new Date());
		
		salvar(faturaNew);
		
		message = "Fatura baixada com sucesso";
		title = "Sucesso";
		
		jsonObject.put("title", title);
		jsonObject.put("message", message);
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(usuarioPermissaoAux -> ResponseEntity.status(200).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	public void enviarEmail(String to, String msg, String subject, Long idFatura) {
		System.out.println("SENDING EMAIL TO " + to);
		
		MimeMessage message = mailSender.createMimeMessage();
		try{
			Configuracao prmFrom = configuracaoRepository.findById(ConfiguracaoHelper.EMAIL_FINANCEIRO).get();
			String emailFrom = "noreply@auttis.com.br";
			if(prmFrom != null && prmFrom.getValor() != null && !prmFrom.getValor().isEmpty())
				emailFrom = prmFrom.getValor();
			simpleMailMessage.setFrom(emailFrom);
			simpleMailMessage.setTo(to);
			
			simpleMailMessage.setSubject(subject);
			
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			
			helper.setFrom(simpleMailMessage.getFrom());
			helper.setTo(simpleMailMessage.getTo());
			helper.setSubject(simpleMailMessage.getSubject());
			helper.setText(msg);
			
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			gerarPDF(idFatura, baos);
			DataSource attachment = new ByteArrayDataSource(baos.toByteArray(), "application/pdf");
			helper.addAttachment("fatura", attachment);
				
	     }catch (MessagingException e) {
		throw new MailParseException(e);
	     }
	     mailSender.send(message);
	         
	}
	
	public void gerarPDF(Long idFatura, OutputStream out) {

//		String fileName =  getClass().getResource("/reports/fatura.jasper").getFile();
		InputStream fileName =  getClass().getResourceAsStream("/reports/fatura.jasper");
		HashMap<String, Object> hm = new HashMap<String, Object>();
		
		File logoFile = null;
		OutputStream outputStream = null;
		
		try {
			
			InputStream pathLogo = getClass().getResourceAsStream("/static/images/logog.png");
			byte[] buffer = new byte[pathLogo.available()];
			
			logoFile = File.createTempFile("logo", ".png");
			outputStream = new FileOutputStream(logoFile);
			
			int read = 0;
			while((read = pathLogo.read(buffer))!= -1 ) {
				outputStream.write(buffer, 0, read);
			}
			
			hm.put("PATH_LOGO", logoFile.getAbsolutePath());
		
			Fatura fatura = get(idFatura);
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			
			hm.put("CODIGO_FATURA", fatura.getId().toString());
			hm.put("CODIGO_FINANCEIRA", fatura.getAverbadora().getCodigo().toString());
			hm.put("NOME_FINANCEIRA", fatura.getAverbadora().getNome());
			hm.put("FECHAMENTO_FATURA", sdf.format(fatura.getDataFechamento()));
			hm.put("VENCIMENTO_FATURA", sdf.format(fatura.getDataVencimento()));
			hm.put("HASH_RELATORIO", Funcoes.gerarHash("fatura sucesso"));
		    hm.put("DATA_POR_EXTENSO", Funcoes.getDataPorExtensoCompleta());
		    hm.put("IP_MAQUINA", httpServletRequest.getRemoteAddr());
		    
		    hm.put("QTD_NOVOS_CONTRATOS", fatura.getQtdNovosContratos());
		    hm.put("VLR_NOVOS_CONTRATOS","R$ " + fatura.getValorNovosContratos().toString());
		    hm.put("QTD_EDIC_CONTRATOS", fatura.getQtdEdicaoContratos());
		    hm.put("VLR_EDIC_CONTRATOS", "R$ " + fatura.getValorEdicao().toString());
		    
		    hm.put("QTD_BAIXAS", fatura.getQtdBaixaContratos());
		    hm.put("VLR_BAIXAS", "R$ " + fatura.getValorBaixaContratos().toString());
		    
		    hm.put("QTD_CONSULTAS", fatura.getQtdConsultaMargem());
		    hm.put("VLR_CONSULTAS", "R$ "+ fatura.getValorConsultaMargem().toString());
		    
		    hm.put("QTD_CANCELAMENTO", fatura.getQtdCancelamentoContratos());
		    hm.put("VLR_CANCELAMENTO", "R$ " + fatura.getValorCancelamentoContratos().toString());
		    
		    hm.put("VLR_TOTAL", "R$ "+fatura.getValorTotal());
			
			List<Bilhetagem> extrato = listarBilhetagens(idFatura);
			
			JasperPrint print= JasperFillManager.fillReport(fileName, hm, new JRBeanCollectionDataSource(extrato));
			JasperExportManager.exportReportToPdfStream(print, out);
 
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				outputStream.close();
				logoFile.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public void gerarExcel(Long idFatura, OutputStream out) {
		
		try {

			List<Bilhetagem> extrato = listarBilhetagens(idFatura);
			
			Workbook workbook = new HSSFWorkbook();
			
			String[] columns = {"NÚM. CONTRATO", "DATA TRANSAÇÃO", "DESCRIÇÃO DA OPERAÇÃO", "FUNCIONARIO"};
			
			CreationHelper createHelper = workbook.getCreationHelper();
			
			Sheet sheet = workbook.createSheet("fatura");
			
	        Font headerFont = workbook.createFont();
	        headerFont.setBold(true);
	        headerFont.setFontHeightInPoints((short) 10);
	        headerFont.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
	
	        CellStyle headerCellStyle = workbook.createCellStyle();
	        headerCellStyle.setFont(headerFont);
	        
	        headerCellStyle.setAlignment(HorizontalAlignment.RIGHT);
	
	        Row headerRow = sheet.createRow(1);
	
	        for(int i = 0; i < columns.length; i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(columns[i]);
	            cell.setCellStyle(headerCellStyle);
	        }
	
	        CellStyle dateCellStyle = workbook.createCellStyle();
	        dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));
	
	        int rowNum = 2;
	        
	        for(Bilhetagem linha: extrato) {
	            Row row = sheet.createRow(rowNum++);
	
	            row.createCell(0)
	                    .setCellValue(linha.getContrato().getNumeroContrato());

	            Cell dataTransacaoCell = row.createCell(1);
	            dataTransacaoCell.setCellValue(linha.getData());
	            dataTransacaoCell.setCellStyle(dateCellStyle);
	            
	            row.createCell(2).setCellValue(linha.getStatusTransacao().getDescricao());
	
	            row.createCell(3)
	                    .setCellValue(linha.getContrato().getFuncionario().getNome());
	        }
	
	        for(int i = 0; i < columns.length; i++) {
	            sheet.autoSizeColumn(i);
	        }
	        
	        
	        rowNum++;
	        rowNum++;
	        rowNum++;
	        
	        Fatura fatura = get(idFatura);
	         Row rowr = sheet.createRow(rowNum++);
	         Cell cellRes = rowr.createCell(0);
	         cellRes.setCellStyle(headerCellStyle);
	         cellRes.setCellValue("RESUMO FATURA");
	         
	         Row rowth = sheet.createRow(rowNum++);
	         rowth.setRowStyle(headerCellStyle);
	         Cell cellh1 = rowth.createCell(1);
	         cellh1.setCellStyle(headerCellStyle);
	         cellh1.setCellValue("QUANTIDADE");
	         Cell cellh2  =rowth.createCell(2);
	         cellh2.setCellStyle(headerCellStyle);
	         cellh2.setCellValue("VALOR");
	         
	         Row rowqn = sheet.createRow(rowNum++);
	         //QTDE NOVOS CONTRATOS
	         Cell cellnq = rowqn.createCell(0);
	         cellnq.setCellStyle(headerCellStyle);
	         cellnq.setCellValue("NOVOS CONTRATOS");
	         //SOMA NOVOS CONTRATOS
	         rowqn.createCell(1).setCellValue(fatura.getQtdNovosContratos());
	         rowqn.createCell(2, CellType.NUMERIC).setCellValue(fatura.getValorNovosContratos().toString());
	         
	         Row rowec = sheet.createRow(rowNum++);
	         //EDICAO CONTRATOS
	         Cell celleq = rowec.createCell(0);
	         celleq.setCellStyle(headerCellStyle);
	         celleq.setCellValue("EDIÇÃO CONTRATOS");	         
	         //SOMA EDICAO CONTRATOS
	         rowec.createCell(1).setCellValue(fatura.getQtdEdicaoContratos());
	         rowec.createCell(2, CellType.NUMERIC).setCellValue(fatura.getValorEdicao().toString());


	         Row rowvb = sheet.createRow(rowNum++);
	         //BAIXAS
	         Cell cellbv =rowvb.createCell(0);
	         cellbv.setCellStyle(headerCellStyle);
	         cellbv.setCellValue("BAIXAS");
	         //SOMA BAIXAS
	         rowvb.createCell(1).setCellValue(fatura.getQtdBaixaContratos().toString());
	         rowvb.createCell(2, CellType.NUMERIC).setCellValue(fatura.getValorBaixaContratos().toString());
	         
	         Row rowvcm = sheet.createRow(rowNum++);
	         //CONSULTAS MARGEM
	         Cell cellvcm = rowvcm.createCell(0);
	         cellvcm.setCellStyle(headerCellStyle);
	         cellvcm.setCellValue("CONSULTAS DE MARGEM");
	         //SOMA CONSULTAS MARGEM
	         rowvcm.createCell(1).setCellValue(fatura.getQtdConsultaMargem().toString());
	         rowvcm.createCell(2, CellType.NUMERIC).setCellValue(fatura.getValorConsultaMargem().toString());
	         
	         Row rowcc = sheet.createRow(rowNum++);
	         //CONSULTAS MARGEM
	         Cell cellcc = rowcc.createCell(0);
	         cellcc.setCellStyle(headerCellStyle);
	         cellcc.setCellValue("CANCELAMENTO DE CONTRATOS");
	         //SOMA CONSULTAS MARGEM
	         rowcc.createCell(1).setCellValue(fatura.getQtdCancelamentoContratos().toString());
	         rowcc.createCell(2, CellType.NUMERIC).setCellValue(fatura.getValorCancelamentoContratos().toString());
	         
	         //VALOR TOTAL
	         Row rowt = sheet.createRow(rowNum++);
	         Cell cellt1 = rowt.createCell(0);
	         cellt1.setCellStyle(headerCellStyle);
	         cellt1.setCellValue("VALOR TOTAL");
	         Cell cellt2 = rowt.createCell(1, CellType.NUMERIC);
	         cellt2.setCellStyle(headerCellStyle);
	         cellt2.setCellValue(fatura.getValorTotal().toString());
	         
	
	        workbook.write(out);
	        out.flush();
	
	        workbook.close();
	        
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public byte[] gerarCSV(Long idFatura) {
		
		try {

			Fatura fatura = get(idFatura);
			List<Bilhetagem> extrato = listarBilhetagens(idFatura);
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			//informacoes da instituicao financeira
			String infosFinan = "";
			infosFinan = infosFinan + "CÓDIGO DA AVERBADORA;" + fatura.getAverbadora().getCodigo().toString() + "\r\n";
			infosFinan = infosFinan + "NOME DA AVERBADORA;" + fatura.getAverbadora().getNome().toString() + "\r\n";
			infosFinan = infosFinan + "FECHAMENTO DA FATURA;" + sdf.format(fatura.getDataFechamento()).toString() + "\r\n";
			infosFinan = infosFinan + "VENCIMENTO DA FATURA;" + sdf.format(fatura.getDataVencimento()).toString() + "\r\n";
			
			//Informacoes do extrato
			String infosExtrato = "";
			for(Bilhetagem m : extrato) {
				infosExtrato = infosExtrato + "EXTRATO;";
				if(m.getContrato().getNumeroContrato() != null) {
					infosExtrato = infosExtrato + m.getContrato().getNumeroContrato().toString() + ";";
				}
				else {
					infosExtrato = infosExtrato + " ;";
				}
				
				infosExtrato = infosExtrato + sdf.format(m.getData()).toString() + ";";
				infosExtrato = infosExtrato + m.getStatusTransacao().getDescricao() + ";";
				infosExtrato = infosExtrato + m.getContrato().getFuncionario().getNome().toString() + "\r\n";
			}
			
			//Resumo da fatura
			String infosResumoFatura = "";
			infosResumoFatura = infosResumoFatura + "NOVOS CONTRATOS;" + fatura.getQtdNovosContratos().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "VALOR NOVOS CONTRATOS;" + fatura.getValorNovosContratos().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "EDIÇÃO DE CONTRATOS;" + fatura.getQtdEdicaoContratos().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "VALOR EDIÇÃO DE CONTRATOS;" + fatura.getValorEdicao().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "BAIXAS;" + fatura.getQtdBaixaContratos().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "VALOR DAS BAIXAS;" + fatura.getValorBaixaContratos().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "CONSULTAS DE MARGEM;" + fatura.getQtdConsultaMargem().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "VALOR DAS CONSULTAS DE MARGEM;" + fatura.getValorConsultaMargem().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "CANCELAMENTO DE CONTRATOS;" + fatura.getQtdCancelamentoContratos().toString() + "\r\n";
			infosResumoFatura = infosResumoFatura + "VALOR DOS CANCELAMENTOS DE CONTRATOS;" + fatura.getValorCancelamentoContratos().toString() + "\r\n";
			
			infosResumoFatura = infosResumoFatura + "TOTAL;" + fatura.getValorTotal().toString() + "\r\n";
			
			String csvString = infosFinan + infosExtrato + infosResumoFatura;
			
			byte[] bytes = csvString.getBytes();
			return bytes;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	public List<Bilhetagem> listarBilhetagens(Long idFatura) throws Exception{
		Bilhetagem filter = new Bilhetagem();
		filter.setFatura(new Fatura());
		filter.getFatura().setId(idFatura);
		return bilhetagemService.pesquisarForFatura(filter);			
	}

	public Fatura salvar(Fatura fatura) {
		if(fatura == null) {
			return null;
		}
		Long idDescricao = fatura.getId() != null && fatura.getDataBaixa() != null ? DescricaoLogAcaoHelper.BAIXA_DE_FATURA_MANUAL : null;
//		List<Log> logs = logAcao(fatura);
		
		Long idFatura = fatura.getId();
		Fatura existing = get(idFatura);
		
		try {
			LogAcao logAcao = funcoes.logAcao(existing.getId(), DescricaoLogAcaoHelper.BAIXA_DE_FATURA_MANUAL, funcoes.getLoggedUser());
			logFatura(existing, fatura, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(fatura, existing);
		Fatura result = repository.save(existing);
		return result;
	}
	
	protected void updateIdLogAcao(List<Log> logs, LogAcao logAcao) throws Exception {
		if(logAcao == null || logs == null || logs.isEmpty())
			return;
		logs.stream().filter(f-> f != null && f.getId() != null).forEach(log -> {
			log.setLogAcao(logAcao);
			logRepository.save(log);
		});
	}
	
//	public LogAcao logAcao(Long idObjeto, Long idDescricao, Usuario usuario) throws Exception {
//		if(idDescricao == null || usuario == null)
//			return null;
//		
// 		LogAcao log = new LogAcao();
//		log.setDataEvento(new Date());
//		DescricaoLogAcao descricao = new DescricaoLogAcao();
//		descricao.setId(idDescricao);		
//		log.setDescricao(descricao);
//		log.setUsuario(usuario);
//		log.setIdObjeto(idObjeto);
//		return logAcaoService.create(log).getBody();
//	}
	
//	public List<Log> logAcao(Fatura fatura) {
//		if(fatura == null || fatura.getId() == null)
//			return null;
//		
//		Fatura existing = repository.findById(fatura.getId()).orElse(null);
//		if(existing == null)
//			return null;
//		
//		List<Log> logs = new ArrayList<Log>();
//		
//		Date datAlteracao = new Date();
//		
//		logUtil.withURL("/cliente/cadastrar").withEntidade("detran");
//		
//		logs.add(logUtil.fromValues("status_fatura", 
//				existing.getStatus() == null || existing.getStatus().isEmpty() ? "" : existing.getStatus(), 
//				fatura.getStatus() == null || existing.getStatus().isEmpty() ? "" : fatura.getStatus(),
//				datAlteracao
//				));
//		
//		logs = logs.stream()
//				.filter(f -> f != null)
//				.map(m -> {
//					m.setIdRow(fatura.getId());
//					return m;
//				})
//				.collect(Collectors.toList());
//		
//		if(logs.isEmpty())
//			return logs;
//		
//		logs.forEach(log -> logRepository.save(log));
//		
//		return logs;
//	}
	
	public void logFatura(Fatura previous, Fatura current, LogAcao logAcao){
		if(previous == null || previous.getId() == null) {
			previous = new Fatura();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		Date datAlteracao = new Date();
		
		logUtil.withEntidade("Financeiro");
		
		logs.add(logUtil.fromValues("id_averbadora", 
				previous.getAverbadora() == null || previous.getAverbadora().getId() == null ? "-" : previous.getAverbadora().getId().toString(), 
				current.getAverbadora() == null || current.getAverbadora().getId() == null ? "-" : current.getAverbadora().getId().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("qtd_novos_contratos_fatura", 
				previous.getQtdNovosContratos() == null ? "-" : previous.getQtdNovosContratos().toString(), 
				current.getQtdNovosContratos() == null ? "-" : current.getQtdNovosContratos().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("vlr_novos_contratos_fatura", 
				previous.getValorNovosContratos() == null ? "-" : previous.getValorNovosContratos().toString(), 
				current.getValorNovosContratos() == null  ? "-" : current.getValorNovosContratos().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("qtd_edicao_contratos_fatura", 
				previous.getQtdEdicaoContratos() == null ? "-" : previous.getQtdEdicaoContratos().toString(), 
				current.getQtdEdicaoContratos() == null ? "-" : current.getQtdEdicaoContratos().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("vlr_edicao_contratos_fatura", 
				previous.getValorEdicao() == null ? "-" : previous.getValorEdicao().toString(), 
				current.getValorEdicao() == null ? "-" : current.getValorEdicao().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("qtd_baixa_contratos_fatura", 
				previous.getQtdBaixaContratos() == null ? "-" : previous.getQtdBaixaContratos().toString(), 
				current.getQtdBaixaContratos() == null ? "-" : current.getQtdBaixaContratos().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("vlr_baixa_contratos_fatura", 
				previous.getValorBaixaContratos() == null ? "-" : previous.getValorBaixaContratos().toString(), 
				current.getValorBaixaContratos() == null ? "-" : current.getValorBaixaContratos().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("qtd_cancel_contr_fatura", 
				previous.getQtdCancelamentoContratos() == null ? "-" : previous.getQtdCancelamentoContratos().toString(), 
				current.getValorCancelamentoContratos() == null ? "-" : current.getValorCancelamentoContratos().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("vlr_cancel_contr_fatura", 
				previous.getValorCancelamentoContratos() == null ? "-" : previous.getValorCancelamentoContratos().toString(), 
				current.getValorCancelamentoContratos() == null ? "-" : current.getValorCancelamentoContratos().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("qtd_consulta_margem_fatura", 
				previous.getQtdConsultaMargem() == null ? "-" : previous.getQtdConsultaMargem().toString(), 
				current.getQtdConsultaMargem() == null ? "-" : current.getQtdConsultaMargem().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("vlr_consulta_margem_fatura", 
				previous.getValorConsultaMargem() == null ? "-" : previous.getValorConsultaMargem().toString(), 
				current.getValorConsultaMargem() == null ? "-" : current.getValorConsultaMargem().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("dt_fechamento_fatura", 
				previous.getDataFechamento() == null ? "-" : sdf.format(previous.getDataFechamento()), 
				current.getDataFechamento() == null ? "-" : sdf.format(current.getDataFechamento()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("dt_vencimento_fatura", 
				previous.getDataVencimento() == null ? "-" : sdf.format(previous.getDataVencimento()), 
				current.getDataVencimento() == null ? "-" : sdf.format(current.getDataVencimento()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("motivo_baixa_fatura", 
				previous.getMotivoBaixa() == null ? "-" : previous.getMotivoBaixa().toString(), 
				current.getMotivoBaixa() == null ? "-" : current.getMotivoBaixa().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("data_baixa_fatura", 
				previous.getDataBaixa() == null ? "-" : sdf.format(previous.getDataBaixa()), 
				current.getDataBaixa() == null ? "-" : sdf.format(current.getDataBaixa()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("status_fatura", 
				previous.getStatus() == null ? "-" : previous.getStatus().toString(), 
				current.getStatus() == null ? "-" : current.getStatus().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("email_enviado", 
				previous.getEmailEnviado() == null ? "-" : previous.getEmailEnviado().toString(), 
				current.getEmailEnviado() == null ? "-" : current.getEmailEnviado().toString(),
				datAlteracao
		));
		
		if(logs.isEmpty()) {
			return;
		}
		
		for(Log log : logs) {
			if(log != null) {
				log.setIdRow(current.getId());
				log.setLogAcao(logAcao);
				logRepository.save(log);
			}
		}
		
	}
}
