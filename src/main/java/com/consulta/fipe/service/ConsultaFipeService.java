package com.consulta.fipe.service;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.consulta.fipe.model.Veiculo;
import com.consulta.fipe.model.VeiculoDetalhe;

@Service
public class ConsultaFipeService {
	
	private String urlFipeApi = "http://fipeapi.appspot.com/api/1/carros/veiculo/";
	
	public ResponseEntity<String> find(Integer idMarca, Integer idVeiculo){
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<List<Veiculo>> responseVeiculos =  restTemplate.exchange(urlFipeApi + idMarca + "/" + idVeiculo + ".json", 
				HttpMethod.GET, 
				null, 
				new ParameterizedTypeReference<List<Veiculo>>() {});
		
		List<Veiculo> listaVeiculos = responseVeiculos.getBody();
		
		if(listaVeiculos == null || listaVeiculos.size() == 0) {
			return ResponseEntity.badRequest().body(null);
		}
		
		for(Veiculo veiculo : listaVeiculos) {
			ResponseEntity<List<VeiculoDetalhe>> responseDetalhe = restTemplate.exchange(urlFipeApi + idMarca + "/" + idVeiculo + "/" + veiculo.getId() + ".json", 
					HttpMethod.GET,
					null, 
					new ParameterizedTypeReference<List<VeiculoDetalhe>>() {});
			
			List<VeiculoDetalhe> listaVeiculoDetalhe = responseDetalhe.getBody();
			for(VeiculoDetalhe veiculoDetalhe : listaVeiculoDetalhe) {
				
			}
			
			return response;
					
		}
		
		return ResponseEntity.ok().body("");
	}
	
}
