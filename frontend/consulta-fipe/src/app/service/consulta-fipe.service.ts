
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Marca } from '../model/marcas';
import { Veiculo } from '../model/veiculos';
import { ConsultaFipe } from '../model/consulta-fipe';
import { Observable } from 'rxjs';

@Injectable()
export class ConsultaFipeService{
    
    constructor(private http: HttpClient){}

    private urlApiConsulta = "http://localhost:8080/consulta.fipe/fipe/marca/";
    private urlApiMarcas = "http://localhost:8080/consulta.fipe/fipe/marcas";
    private urlApiVeiculos = "http://localhost:8080/consulta.fipe/fipe/veiculos/";

    getMarcas(): Observable<Marca[]>{
        return this.http.get<Marca[]>(this.urlApiMarcas);
    }

    getVeiculos(idMarca): Observable<Veiculo[]>{
        return this.http.get<Veiculo[]>(this.urlApiVeiculos + idMarca);
    }

    getDetalhe(idMarca, idVeiculo): Observable<ConsultaFipe[]>{
        return this.http.get<ConsultaFipe[]>(this.urlApiConsulta + idMarca + "/modelo/" + idVeiculo);
    }
}