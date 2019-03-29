package com.sif.core.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.exception.GenericException;
import com.sif.core.service.UsuarioService;
import com.sif.core.utils.ConfiguracaoHelper;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.PerfilHelper;
import com.sif.model.Averbadora;
import com.sif.model.Configuracao;
import com.sif.model.Consignataria;
import com.sif.model.Contrato;
import com.sif.model.Orgao;
import com.sif.model.Perfil;
import com.sif.model.Usuario;
import com.sif.model.custom.ContratoRelatorioDto;
import com.sif.model.custom.RelatorioDto;
import com.sif.model.custom.SubrelatorioContrato;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.ContratoRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.specification.AverbadoraSpecification;
import com.sif.repository.specification.ConsignatariaSpecification;
import com.sif.repository.specification.OrgaoSpecification;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@RestController
@RequestMapping("/relatorio")
public class RelatorioController {
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	
	@Autowired
	ContratoRepository contratoRepository;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
	ServletContext context;
	
	@PostMapping("/consolidado")
	@PreAuthorize("hasAuthority('/relatorio/new')")
	public void relatorioConsolidado(@RequestBody RelatorioDto dto, HttpServletRequest request, 
			HttpServletResponse response) {
		
		if(dto.getPeriodo() == 0) {
			throw new GenericException("Erro", "Periodo deve ser preenchido");
		}
	
		InputStream fileName = getClass().getResourceAsStream("/reports/relatorio_consolidado.jasper");
		
//		String fileName =  getClass().getResource("/reports/relatorio_consolidado.jasper").getFile();
		HashMap parameters_consolidado = new HashMap();
		
		File logoFile = null;
		OutputStream outputStream = null;
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			
			InputStream pathLogo = getClass().getResourceAsStream("/static/images/logog.png");
			byte[] buffer = new byte[pathLogo.available()];
			
			logoFile = File.createTempFile("logo", ".png");
			outputStream = new FileOutputStream(logoFile);
			
			int read = 0;
			while((read = pathLogo.read(buffer))!= -1 ) {
				outputStream.write(buffer, 0, read);
			}
			
			parameters_consolidado.put("PATH_LOGO", logoFile.getAbsolutePath());
			parameters_consolidado.put("TIPO_RELATORIO", "Consolidado");
			parameters_consolidado.put("PERIODO", dto.getPeriodo() == 0 ? "Mensal - " + dto.getMesAno() : getDataIniFim(dto));
			parameters_consolidado.put("DATA_EMISSAO", sdf.format(new Date()));
			parameters_consolidado.put("NIVEL_RELATORIO", getTipoEntidade());
			parameters_consolidado.put("UF_RELATORIO", getUfRelatorio());
			
			List<Contrato> contratos = pesquisar(dto);
			
			Map<Long, ContratoRelatorioDto> financeiras = new HashMap<Long, ContratoRelatorioDto>();
			
			contratos.forEach(c -> {
				if(financeiras.get(c.getUsuario().getEntidade()) == null) {
					
					String[] dados = getDadosEntidade(c.getUsuario());
					
					String codigo = dados[0];
					String nomeInstituicao = dados[1];
					String tipoInstituicao = dados[2];
					
					ContratoRelatorioDto rel = new ContratoRelatorioDto();
					rel.setCodFinanceira(codigo);
					rel.setNomeFinanceira(nomeInstituicao);
					rel.setNumContratos(0L);
					financeiras.put(c.getUsuario().getEntidade(), rel);
				}
				
				ContratoRelatorioDto i = financeiras.get(c.getUsuario().getEntidade());
				i.setNumContratos(i.getNumContratos() + 1L);
			});
			
			List<ContratoRelatorioDto> list = new LinkedList<ContratoRelatorioDto>();
			
			Iterator it = financeiras.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        System.out.println(pair.getKey() + " = " + pair.getValue());
		        list.add((ContratoRelatorioDto)pair.getValue());
		    }
		    
		    parameters_consolidado.put("SOMA_INSTITUICAO", list.size());
		    
		    String ipMaquina = InetAddress.getLocalHost().getHostAddress();
		    
		    parameters_consolidado.put("HASH_RELATORIO", Funcoes.gerarHash("relatorio sucesso"));
		    parameters_consolidado.put("DATA_POR_EXTENSO", Funcoes.getDataPorExtensoCompleta());
		    parameters_consolidado.put("IP_MAQUINA", ipMaquina);
		    
		    
		    JasperPrint print= JasperFillManager.fillReport(fileName, parameters_consolidado, new JRBeanCollectionDataSource(list));
			
			response.setContentType("application/x-pdf");
			response.setHeader("Content-Disposition", "attachment; filename=relatorio-consolidado.pdf");
			JasperExportManager.exportReportToPdfStream(print, response.getOutputStream());
//			byte[] bytes = JasperExportManager.exportReportToPdf(print);
//			
//			return ResponseEntity.ok()
//					.header("Content-Type", "application/pdf; charset=UTF-8")
//					.header("Content-Disposition", "inline; filename=relatorio-consolidado.pdf")
//					.body(bytes);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
//		return ResponseEntity.badRequest().body(null);
	}
	
	@PostMapping("/completo")
	@PreAuthorize("hasAuthority('/relatorio/new')")
	public void relatorioCompleto(@RequestBody RelatorioDto rel, HttpServletRequest request, 
			HttpServletResponse response) {
		
		if(rel.getPeriodo() == 0) {
			throw new GenericException("Erro", "Periodo deve ser preenchido");
		}
		
		InputStream fileName = getClass().getResourceAsStream("/reports/relatorio_completo.jasper");
		InputStream fileSubreportDir = getClass().getResourceAsStream("/reports/");
		
//		String fileName =  getClass().getResource("/reports/relatorio_completo.jasper").getFile();
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
			hm.put("TIPO_RELATORIO", "Completo");
			hm.put("PERIODO", rel.getPeriodo() == 0 ? "Mensal - " + rel.getMesAno() : getDataIniFim(rel));
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			hm.put("DATA_EMISSAO", sdf.format(new Date()));
			hm.put("NIVEL_RELATORIO", getTipoEntidade());
			hm.put("UF_RELATORIO", getUfRelatorio());
			hm.put("SUBREPORT_CONTRATOS_CLIENTE", "contratos_cliente.jasper");
//			hm.put("SUBREPORT_DIR", getClass().getResource("/reports/").getFile());
			hm.put("SUBREPORT_DIR", context.getRealPath("/reports").concat(File.separator));
			
			List<Contrato> contratos = pesquisar(rel);
			
			Map<Long, ContratoRelatorioDto> financeiras = new HashMap<Long, ContratoRelatorioDto>();
			
			contratos.forEach(c -> {
				if(financeiras.get(c.getUsuario().getEntidade()) == null) {
					
					
					
					String[] dados = getDadosEntidade(c.getUsuario());
					
					String codigo = dados[0];
					String nomeInstituicao = dados[1];
					String tipoInstituicao = dados[2];
					
					ContratoRelatorioDto dto = new ContratoRelatorioDto();
					dto.setCodFinanceira(codigo);
					dto.setNomeFinanceira(nomeInstituicao);
					dto.setNumContratos(0L);
					dto.setTipoEntidade(tipoInstituicao);
					
					dto.setContratos(contratos.stream()
							.filter(f -> f.getUsuario().getEntidade().equals(c.getUsuario().getEntidade()))
							.map(m -> {
								
								String[] dadosEntidade = getDadosEntidade(m.getUsuario());
								SubrelatorioContrato cdto = new SubrelatorioContrato();  
								cdto.setDocumentoCliente(m.getUsuario().getDocumento());
								cdto.setNomeCliente(m.getUsuario().getNome());
								cdto.setNumeroContrato(m.getNumeroContrato());
								cdto.setDataRegistro(sdf.format(m.getDataLancamento()));
								cdto.setTipo(dadosEntidade[1]);
								return cdto;
							}).collect(Collectors.toList()));
					
					financeiras.put(c.getUsuario().getEntidade(), dto);
				}
				
				ContratoRelatorioDto i = financeiras.get(c.getUsuario().getEntidade());
				i.setNumContratos(i.getNumContratos() + 1L);
			});
			
			List<ContratoRelatorioDto> list = new LinkedList<ContratoRelatorioDto>();
			
			Iterator it = financeiras.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        System.out.println(pair.getKey() + " = " + pair.getValue());
		        list.add((ContratoRelatorioDto)pair.getValue());
		    }
		    
		    String ipMaquina = InetAddress.getLocalHost().getHostAddress();
		    
		    hm.put("SOMA_INSTITUICAO", list.size());
		    hm.put("HASH_RELATORIO", Funcoes.gerarHash("relatorio sucesso"));
		    hm.put("DATA_POR_EXTENSO", Funcoes.getDataPorExtensoCompleta());
		    hm.put("IP_MAQUINA", ipMaquina);
		    
			JasperPrint print = JasperFillManager.fillReport(fileName, hm, new JRBeanCollectionDataSource(list));
			response.setContentType("application/x-pdf");
			response.setHeader("Content-Disposition", "attachment; filename=relatorio-completo.pdf");
			JasperExportManager.exportReportToPdfStream(print, response.getOutputStream());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getDataIniFim(RelatorioDto filter) {
		
		Date dtIni = null;
		Date dtFim = null;
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		
		try {
			if(filter.getPeriodo() == 0) {
				if(filter.getMesAno() != null && !filter.getMesAno().isEmpty()) {
				String mes = filter.getMesAno().substring(0, filter.getMesAno().indexOf("/"));				
				String ano = filter.getMesAno().substring(filter.getMesAno().indexOf("/") +1);
				String dti = "01/"+mes+"/"+ano;				
				dtIni = sdf.parse(dti);
				Calendar cal = Calendar.getInstance();
				cal.setTime(dtIni);
				cal.add(Calendar.MONTH, 1);
				dtFim = cal.getTime();
				}
			} else {
				dtIni = filter.getDataInicial() == null || filter.getDataInicial().isEmpty() ? null : sdf.parse(filter.getDataInicial());
				dtFim = filter.getDataFinal() == null  || filter.getDataFinal().isEmpty() ? null : sdf.parse(filter.getDataFinal());
			}
			return sdf.format(dtIni) + " - " + sdf.format(dtFim);
			} catch (ParseException e) {
			e.printStackTrace();
		}
		return "";
		
	}
	
	private String getTipoEntidade() {
		
		Usuario usuario = funcoes.getLoggedUser();
		
		Perfil perfil = usuario.getPerfil();		
		if(perfil.getId().equals(PerfilHelper.ORGAO)){
			return "ORGÃO";
		}
		else if(perfil.getId().equals(PerfilHelper.AVERBADORA)){
			return "AVERBADORA";
		}
		else if(perfil.getId().equals(PerfilHelper.CONSIGNATARIA)){
			return "CONSIGNATÁRIA";
		}
		return "ADMIN";
	}
	
	private String getUfRelatorio() {
		
		String result = "";
		
		Configuracao pUf = configuracaoRepository.findById(ConfiguracaoHelper.UF_SISTEMA).get();
		if(pUf != null && pUf.getValor() != null)
			result = pUf.getValor();
		
		return result;
	}
	
	private List<Contrato> pesquisar(RelatorioDto filter) {
		if(filter == null)
			filter = new RelatorioDto();
		List<Contrato> lista = new ArrayList<Contrato>();
		Usuario usuarioSessao = funcoes.getLoggedUser();
		
		Perfil perfil = usuarioSessao.getPerfil();
		
		Long instituicaoId = null;
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

		Date dtIni = null;
		Date dtFim = null;
		
		try {
			if(filter.getPeriodo() == 0) {
				if(filter.getMesAno() != null && !filter.getMesAno().isEmpty()) {
					String mes = filter.getMesAno().substring(0, filter.getMesAno().indexOf("/"));				
					String ano = filter.getMesAno().substring(filter.getMesAno().indexOf("/") +1);
					String dti = "01/"+mes+"/"+ano;				
					dtIni = sdf.parse(dti);
					Calendar cal = Calendar.getInstance();
					cal.setTime(dtIni);
					cal.add(Calendar.MONTH, 1);
					dtFim = cal.getTime();
				}
			} else {
				dtIni = filter.getDataInicial() == null || filter.getDataInicial().isEmpty() ? null : sdf.parse(filter.getDataInicial());
				dtFim = filter.getDataFinal() == null  || filter.getDataFinal().isEmpty() ? null : sdf.parse(filter.getDataFinal());
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if(perfil.getId().equals(PerfilHelper.ORGAO)) {
			List<Usuario> usuarios = usuarioService.findByPerfilEntidade(PerfilHelper.ORGAO, usuarioSessao.getEntidade());
			for(Usuario user : usuarios) {
				lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
			}
			
			usuarios = new ArrayList<Usuario>();
			List<Averbadora> averbadoras = averbadoraRepository.findByOrgao(usuarioSessao.getEntidade());
			
			List<Consignataria> consignatarias = new ArrayList<Consignataria>(); 
			for(Averbadora averb : averbadoras) {
				usuarios.addAll(usuarioService.findByPerfilEntidade(PerfilHelper.AVERBADORA, averb.getId()));
				consignatarias.addAll(consignatariaRepository.findByAverbadora(averb.getId()));
				for(Usuario user : usuarios) {
					lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
				}
				
				usuarios = new ArrayList<Usuario>();
			}
			
			for(Consignataria consig : consignatarias) {
				usuarios.addAll(usuarioService.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consig.getId()));
				for(Usuario user : usuarios) {
					lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
				}
				usuarios = new ArrayList<Usuario>();
			}
		} 
		else if(perfil.getId().equals(PerfilHelper.AVERBADORA)) {
			List<Usuario> usuarios = usuarioService.findByPerfilEntidade(PerfilHelper.AVERBADORA, usuarioSessao.getEntidade());
			List<Consignataria> consignatarias = consignatariaRepository.findByAverbadora(usuarioSessao.getEntidade());
			for(Usuario user : usuarios) {
				lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
			}
			usuarios = new ArrayList<Usuario>();
			for(Consignataria consig : consignatarias) {
				usuarios.addAll(usuarioService.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consig.getId()));
				for(Usuario user : usuarios) {
					lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
				}
				usuarios = new ArrayList<Usuario>();
			}
		} 
		else if(perfil.getId().equals(PerfilHelper.CONSIGNATARIA)){
			List<Usuario> usuarios = usuarioService.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, usuarioSessao.getEntidade());
			for(Usuario user : usuarios) {
				lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
			}
		}
		else {
			List<Orgao> orgaos = orgaoRepository.findAll(Specification.where(new OrgaoSpecification().findByStatus()));
			List<Averbadora> averbadoras = averbadoraRepository.findAll(Specification.where(new AverbadoraSpecification().findByStatus()));		
			List<Consignataria> consignatarias = consignatariaRepository.findAll(Specification.where(new ConsignatariaSpecification().findByStatus()));
		
			for(Orgao orgao : orgaos) {
				List<Usuario> usuarioOrgao = usuarioService.findByPerfilEntidade(PerfilHelper.ORGAO, orgao.getId());
				for(Usuario user : usuarioOrgao) {
					lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
				}
				usuarioOrgao = new ArrayList<Usuario>();
			}
			for(Averbadora averbadora : averbadoras) {
				List<Usuario> usuarioAverbadora = usuarioService.findByPerfilEntidade(PerfilHelper.AVERBADORA, averbadora.getId());
				for(Usuario user : usuarioAverbadora) {
					lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
				}
				usuarioAverbadora = new ArrayList<Usuario>();
			}
			for(Consignataria consignataria : consignatarias) {
				List<Usuario> usuarioConsignataria = usuarioService.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consignataria.getId());
				for(Usuario user : usuarioConsignataria) {
					lista.addAll(contratoRepository.findContratosByYearAndUsuario(dtIni, dtFim, user.getId()));
				}
				usuarioConsignataria = new ArrayList<Usuario>();
			}
		}
		return lista;
	}
	
	private String[] getDadosEntidade(Usuario usuario) {
		
		String[] retorno = new String[3];
		String codigo = "";
		String nomeInstituicao = "";
		String tipoInstituicao = "";
		
		if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
			Orgao orgao = orgaoRepository.findById(usuario.getEntidade()).get();
			codigo = "";
			nomeInstituicao = orgao.getNome();
			tipoInstituicao = "ORGÃO";
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
			Averbadora averbadora = averbadoraRepository.findById(usuario.getEntidade()).get();
			codigo = averbadora.getCodigo().toString();
			nomeInstituicao = averbadora.getNome();
			tipoInstituicao = "AVERBADORA";
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
			Consignataria consignataria = consignatariaRepository.findById(usuario.getEntidade()).get();
			codigo = consignataria.getCodigo();
			nomeInstituicao = consignataria.getNome();
			tipoInstituicao = "CONSIGNATÁRIA";
		}
		
		retorno[0] = codigo;
		retorno[1] = nomeInstituicao;
		retorno[2] = tipoInstituicao;
		
		return retorno;
	}
}
