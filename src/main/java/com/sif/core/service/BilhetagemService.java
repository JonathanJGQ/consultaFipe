package com.sif.core.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.sif.model.Bilhetagem;
import com.sif.model.Contrato;
import com.sif.model.DescricaoHistoricoExtrato;
import com.sif.model.Usuario;
import com.sif.repository.BilhetagemRepository;
import com.sif.repository.specification.BilhetagemSpecification;

@Service
public class BilhetagemService {
	
	@Autowired
	BilhetagemRepository repository;
	
	public Bilhetagem salvar(Bilhetagem bilhetagem) {
		return repository.save(bilhetagem);
	}
	
	public Bilhetagem gerar(Contrato contrato, DescricaoHistoricoExtrato desc, Usuario usuario) throws Exception{
		return gerar(contrato, desc, usuario, 0);
	}
	
	public Bilhetagem gerar(Contrato contrato, DescricaoHistoricoExtrato desc, Usuario usuario, int status) throws Exception{
		if(contrato == null || contrato.getId() == null)
			return null;
		Bilhetagem bilhetagem = new Bilhetagem();
		bilhetagem.setContrato(contrato);
		bilhetagem.setUsuario(usuario);
		bilhetagem.setAverbadora(contrato.getVerba().getConsignataria().getAverbadora());
		bilhetagem.setStatusTransacao(desc);
		bilhetagem.setData(new Date());
		bilhetagem.setStatus(status);
		bilhetagem.setHash("");
		return salvar(bilhetagem);
	}
	
	public List<Bilhetagem> pesquisar(Bilhetagem filter) {
		if(filter == null || filter.getFatura() == null || filter.getFatura().getId() == null)
			return Collections.emptyList();
		BilhetagemSpecification spec = new BilhetagemSpecification();
		return repository.findAll(spec.idFaturaEq(filter.getFatura().getId()));
	}
	
	public List<Bilhetagem> pesquisarForFatura(Bilhetagem filter) {
		if(filter == null || filter.getFatura() == null || filter.getFatura().getId() == null)
			return Collections.emptyList();
		BilhetagemSpecification spec = new BilhetagemSpecification();
		return repository.findAll(Specification.where(
				spec.idFaturaEq(filter.getFatura().getId())
			).and(spec.bilhetagemParaFatura())
		);
	}
}
