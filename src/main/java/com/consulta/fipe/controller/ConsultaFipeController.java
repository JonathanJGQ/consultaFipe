package com.consulta.fipe.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.consulta.fipe.model.ConsultaFipe;
import com.consulta.fipe.model.Marca;
import com.consulta.fipe.model.Veiculo;
import com.consulta.fipe.service.ConsultaFipeService;


@RestController
@RequestMapping("/fipe")
public class ConsultaFipeController {
	
	@Autowired
	ConsultaFipeService consultaFipeService;
	
	@GetMapping("/marca/{idMarca}/modelo/{idModelo}")
	public ResponseEntity<List<ConsultaFipe>> findFipe(@PathVariable() Integer idMarca, @PathVariable() Integer idModelo) {
		return consultaFipeService.findFipe(idMarca, idModelo);
	}
	
	@GetMapping("/marcas")
	public ResponseEntity<List<Marca>> findMarcas() {
		return consultaFipeService.getMarcas();
	}
	
	@GetMapping("/veiculos/{idMarca}")
	public ResponseEntity<List<Veiculo>> findVeiculos(@PathVariable() Integer idMarca) {
		return consultaFipeService.getVeiculos(idMarca);
	}
}
