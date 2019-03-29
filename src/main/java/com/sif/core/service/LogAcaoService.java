package com.sif.core.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.model.DescricaoLogAcao;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.repository.DescricaoLogAcaoRepository;
import com.sif.repository.LogAcaoRepository;
import com.sif.repository.specification.LogAcaoSpecification;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class LogAcaoService {

	@Autowired
	LogAcaoRepository logAcaoRepository;
	
	@Autowired
	LogService logSercice;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	DescricaoLogAcaoRepository descricaoRepository;
	
	public Page<LogAcao> getAll(Pageable pageable, LogAcao logAcao) {
		
//		List<LogAcaoDTO> listaRetorno = new ArrayList<LogAcaoDTO>();
		
		if(logAcao.getDataFinal() != null) {
			Calendar calLe = Calendar.getInstance();
			calLe.setTime(logAcao.getDataFinal());
			calLe.add(Calendar.DAY_OF_MONTH, 1);
			logAcao.setDataFinal(calLe.getTime());
		}
		
		String nome = null;
		if(logAcao.getUsuario() != null) {
			nome = logAcao.getUsuario().getNome();
		}
		
		LogAcaoSpecification spec = new LogAcaoSpecification();
		Page<LogAcao> lista = logAcaoRepository.findAll(Specification.where(
				spec.nomeUsuarioLike(nome))
				.and(spec.dataLogGe(logAcao.getDataInicial()))
				.and(spec.dataLogLe(logAcao.getDataFinal()))
				.and(spec.orderById())
				.and(spec.descricaoEquals(logAcao.getDescricao() == null ? null : logAcao.getDescricao().getId())),pageable);	
		
//		for(LogAcao objeto : lista) {
//			LogAcaoDTO entity = new LogAcaoDTO();
//			entity.setId(objeto.getId());
//			entity.setDescricao(objeto.getDescricao().getDescricao());
//			entity.setDataEvento(objeto.getDataEvento());
//			entity.setIdObjeto(objeto.getIdObjeto());
//			entity.setUsuario(objeto.getUsuario().getNome());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public List<LogAcao> pesquisa(LogAcao logAcao) {
		
		if(logAcao.getDataFinal() != null) {
			Calendar calLe = Calendar.getInstance();
			calLe.setTime(logAcao.getDataFinal());
			calLe.add(Calendar.DAY_OF_MONTH, 1);
			logAcao.setDataFinal(calLe.getTime());
		}
		
		String nome = null;
		if(logAcao.getUsuario() != null) {
			nome = logAcao.getUsuario().getNome();
		}
		
		LogAcaoSpecification spec = new LogAcaoSpecification();
		List<LogAcao> lista = logAcaoRepository.findAll(Specification.where(
				spec.nomeUsuarioLike(nome)
			).and(spec.dataLogGe(logAcao.getDataInicial()))
			.and(spec.dataLogLe(logAcao.getDataFinal()))
			.and(spec.orderById()));	
		
		return lista;
	}
	
	public ResponseEntity<LogAcao> findById(Long id) {
		
		return Optional
				.ofNullable(logAcaoRepository.findById(id).get())
				.map(verbaAux -> ResponseEntity.ok().body(verbaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<LogAcao> create(LogAcao logAcao) {
		
		validarLogAcao(logAcao);
		
		return Optional
				.ofNullable(logAcaoRepository.save(logAcao))
				.map(logAcaoAux -> ResponseEntity.ok().body(logAcaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<LogAcao> update(LogAcao logAcao,Long id) {
		
		validarLogAcao(logAcao);
		
		LogAcao logAcaoSave = logAcaoRepository.findById(id).get();
		if(logAcaoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long verbaId = logAcaoSave.getId();
		
		BeanUtils.copyProperties(logAcao, logAcaoSave);
		logAcaoSave.setId(verbaId);
		
		return Optional
				.ofNullable(logAcaoRepository.save(logAcaoSave))
				.map(logAcaoAux -> ResponseEntity.ok().body(logAcaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarLogAcao(LogAcao logAcao) throws GenericException {
		
		if(logAcao.getDataEvento() == null) {
			throw new GenericException("Erro","Data não pode ser nulo");
		}
		if(logAcao.getDescricao() == null) {
			throw new GenericException("Erro","Descrição não pode ser nulo");
		}
//		if(logAcao.getIdObjeto() == null) {
//			throw new GenericException("Erro","Id Objeto não pode ser nulo");
//		}
		if(logAcao.getUsuario() == null) {
			throw new GenericException("Erro","Usuário não pode ser nulo");
		}
		
	}
	
	public ResponseEntity<List<Log>> getAuditoriaByLogAcao(String id){
		LogAcaoSpecification logAcaoSpec = new LogAcaoSpecification();
		
		Long idAcao = null;
		if(id != null && !id.isEmpty()) {
			idAcao = Long.parseLong(id);
		} else {
			throw new GenericException("Erro","O id não pode ser nulo");
		}
		
		
		List<Log> logsAuditoria = logSercice.findByLogAcao(idAcao);
		
		return Optional
				.ofNullable(logsAuditoria)
				.map(logAcaoAux -> ResponseEntity.ok().body(logAcaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public void gerarPDF(LogAcao certidao, HttpServletResponse response) {
		
		try {
			InputStream fileName =  getClass().getResourceAsStream("/reports/logacao.jasper");
			HashMap<String, Object> hm = new HashMap<String, Object>();
			
			
			InputStream fileImage = getClass().getResourceAsStream("/static/images/logog.png");
			byte[] buffer = new byte[fileImage.available()];
			
			File logoFile = null;
			OutputStream outputStream = null;
			logoFile = File.createTempFile("logo", ".png");
			outputStream = new FileOutputStream(logoFile);
			
			int read = 0;
			while((read = fileImage.read(buffer))!= -1 ) {
				outputStream.write(buffer, 0, read);
			}
			hm.put("PATH_LOGO", logoFile.getAbsolutePath());
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			hm.put("DATA_EMISSAO", sdf.format(new Date()));
		    hm.put("DATA_POR_EXTENSO", funcoes.getDataPorExtensoCompleta());
		    
		    String ipMaquina = InetAddress.getLocalHost().getHostAddress();
		    hm.put("IP_MAQUINA", ipMaquina);
		    
		    Calendar calendar = Calendar.getInstance();
		    calendar.add(Calendar.DAY_OF_MONTH, 30);
		    Date validade = calendar.getTime();
		    
			hm.put("DATA_VALIDADE", sdf.format(validade));
			
			LogAcaoSpecification spec = new LogAcaoSpecification();
//			List<LogAcao> list = pesquisa(certidao);
			List<LogAcao> list =logAcaoRepository.findAll(Specification.where(
					spec.nomeUsuarioLike(certidao.getUsuario() == null ? null : certidao.getUsuario().getNome()))
					.and(spec.dataLogGe(certidao.getDataInicial()))
					.and(spec.dataLogLe(certidao.getDataFinal()))
					.and(spec.orderById())
					.and(spec.descricaoEquals(certidao.getDescricao() == null ? null : certidao.getDescricao().getId())));
			
			JasperPrint print = JasperFillManager.fillReport(fileName, hm, new JRBeanCollectionDataSource(list));
			response.setContentType("application/x-pdf");
			response.setHeader("Content-Disposition", "attachment; filename=log_acao.pdf");
			JasperExportManager.exportReportToPdfStream(print, response.getOutputStream());
//			JasperExportManager.exportReportToPdf(print);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void gerarPDFDetalhes(LogAcao logAcao, HttpServletResponse response) {
		try {
			InputStream fileName =  getClass().getResourceAsStream("/reports/logacao_detalhes.jasper");
			HashMap<String, Object> hm = new HashMap<String, Object>();
			
			InputStream fileImage = getClass().getResourceAsStream("/static/images/logog.png");
			byte[] buffer = new byte[fileImage.available()];
			
			File logoFile = null;
			OutputStream outputStream = null;
			logoFile = File.createTempFile("logo", ".png");
			outputStream = new FileOutputStream(logoFile);
			
			int read = 0;
			while((read = fileImage.read(buffer))!= -1 ) {
				outputStream.write(buffer, 0, read);
			}
			hm.put("PATH_LOGO", logoFile.getAbsolutePath());
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			hm.put("DATA_EMISSAO", sdf.format(new Date()));
		    hm.put("DATA_POR_EXTENSO", funcoes.getDataPorExtensoCompleta());
		    
		    String ipMaquina = InetAddress.getLocalHost().getHostAddress();
		    hm.put("IP_MAQUINA", ipMaquina);
		    
		    Calendar calendar = Calendar.getInstance();
		    calendar.add(Calendar.DAY_OF_MONTH, 30);
		    Date validade = calendar.getTime();
		    
			hm.put("DATA_VALIDADE", sdf.format(validade));
			
			List<Log> detalhes = logSercice.findByLogAcao(logAcao.getId());
			
			JasperPrint print = JasperFillManager.fillReport(fileName, hm, new JRBeanCollectionDataSource(detalhes));
			response.setContentType("application/x-pdf");
			response.setHeader("Content-Disposition", "attachment; filename=log_acao.pdf");
			JasperExportManager.exportReportToPdfStream(print, response.getOutputStream());
			
		} catch (Exception e) {
			
		}
	}
	
	public DescricaoLogAcao getDescricaoLogAcao(Long id) {
		if(id == null)
			return null;
		return descricaoRepository.findById(id).orElse(null);
	}
	
}
