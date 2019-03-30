import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from "@angular/forms";

import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AlertModule } from 'ngx-bootstrap';
import { AppComponent } from './app.component';
import { ConsultaComponent } from './consulta/consulta.component';
import { ConsultaFipeService } from './service/consulta-fipe.service';

@NgModule({
  declarations: [
    AppComponent,
    ConsultaComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    AlertModule.forRoot()
  ],
  providers: [
    ConsultaFipeService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
