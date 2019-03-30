package com.consulta.fipe.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.consulta.fipe.model.ConsultaFipe;
import com.consulta.fipe.model.Marca;
import com.consulta.fipe.model.Veiculo;
import com.consulta.fipe.model.VeiculoDetalhe;

@Service
public class ConsultaFipeService {
	
	private String urlFipeApi = "http://fipeapi.appspot.com/api/1/carros/veiculo/";
	
	private String urlMarcasApi = "http://fipeapi.appspot.com/api/1/carros/marcas.json";
	
	private String urlVeiculosApi = "http://fipeapi.appspot.com/api/1/carros/veiculos/";
	
	public ResponseEntity<List<ConsultaFipe>> findFipe(Integer idMarca, Integer idVeiculo){
		
		List<ConsultaFipe> listaConsultaFipe = new ArrayList<ConsultaFipe>();
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<List<Veiculo>> responseVeiculos =  restTemplate.exchange(urlFipeApi + idMarca + "/" + idVeiculo + ".json", 
				HttpMethod.GET, 
				null, 
				new ParameterizedTypeReference<List<Veiculo>>() {});
		
		List<Veiculo> listaVeiculos = responseVeiculos.getBody();
		
		if(listaVeiculos == null || listaVeiculos.size() == 0) {
			return ResponseEntity.badRequest().body(null);
		}
		
		BigDecimal valorAntigo = new BigDecimal("0");
		for(Veiculo veiculo : listaVeiculos) {
			VeiculoDetalhe veiculoDetalhe = restTemplate.getForObject(urlFipeApi + idMarca + "/" + idVeiculo + "/" + veiculo.getId() + ".json",VeiculoDetalhe.class);
			
			String strPreco = veiculoDetalhe.getPreco().replace("R$ ", "").replace(".", "").replace(",", ".");
			BigDecimal preco = new BigDecimal(strPreco);
			ConsultaFipe consultaFipe = new ConsultaFipe();
			consultaFipe.setAno(veiculoDetalhe.getAno_modelo());
			consultaFipe.setValor("R$ " + preco.toString());
			
			if(valorAntigo.compareTo(new BigDecimal("0")) != 0 ) {
				BigDecimal diferenca = preco.subtract(valorAntigo).abs();
				consultaFipe.setDiferencaValor("R$ " + diferenca.toString());
				BigDecimal porcentagem = diferenca.multiply(new BigDecimal("100")).divide(valorAntigo, 2, RoundingMode.HALF_EVEN);
				consultaFipe.setPorcentagem(String.valueOf(porcentagem.doubleValue() + "%"));
			}
			else {
				consultaFipe.setDiferencaValor("R$ 0.00");
			}
			
			valorAntigo = preco;
			
			listaConsultaFipe.add(consultaFipe);
			
					
		}
		
		return ResponseEntity.ok().body(listaConsultaFipe);
	}
	
	public ResponseEntity<List<Marca>> getMarcas(){
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<List<Marca>> responseMarcas =  restTemplate.exchange(urlMarcasApi, 
				HttpMethod.GET, 
				null, 
				new ParameterizedTypeReference<List<Marca>>() {});
		
		List<Marca> listaMarcas = responseMarcas.getBody();
		listaMarcas.sort(Comparator.comparing(Marca::getName));
		return ResponseEntity.ok().body(listaMarcas);
	}
	
	public ResponseEntity<List<Veiculo>> getVeiculos(Integer idMarca){
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<List<Veiculo>> responseVeiculos =  restTemplate.exchange(urlVeiculosApi + idMarca + ".json", 
				HttpMethod.GET, 
				null, 
				new ParameterizedTypeReference<List<Veiculo>>() {});
		
		List<Veiculo> listaVeiculo = responseVeiculos.getBody();
		listaVeiculo.sort(Comparator.comparing(Veiculo::getName));
		return ResponseEntity.ok().body(listaVeiculo);
	}
	
}
