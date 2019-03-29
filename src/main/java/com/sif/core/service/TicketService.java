package com.sif.core.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.core.utils.TipoPerfil;
import com.sif.model.Administracao;
import com.sif.model.Averbadora;
import com.sif.model.Consignataria;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Orgao;
import com.sif.model.Perfil;
import com.sif.model.Usuario;
import com.sif.model.custom.TicketDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.AdministracaoRepository;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.UsuarioRepository;

@Service
public class TicketService {
	
	private final static String API_TOKEN = "wjPAV7dldjtZQc7WyvfX";
	private final static String ENDPOINT = "https://auttis.freshdesk.com";
	private final static String COMPANY = ENDPOINT + "/api/v2/companies";
	private final static String CONTACT = ENDPOINT + "/api/v2/contacts";
	private final static String TICKET = ENDPOINT + "/api/v2/tickets";
	private final static String TICKET_SEARCH = ENDPOINT + "/api/v2/search/tickets";
	
	private final static String USER_SEARCH = ENDPOINT + "/api/v2/search/contacts";
	
	private final static String RESPONDER_ID = "43009131164";

	@Autowired
	AverbadoraService averbadoraService;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	OrgaoService orgaoService;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	AdministracaoService administracaoService;
	
	@Autowired
	AdministracaoRepository administracaoRepository;
	
	@Autowired
	ConsignatariaService consignatariaService;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	HttpClientBuilder hcBuilder;
	RequestBuilder reqBuilder;
	RequestConfig.Builder rcBuilder;
	
	public boolean createEntity(Object object) throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.post();
		rcBuilder = RequestConfig.custom();
		
		Averbadora averbadora = new Averbadora();
		Orgao orgao = new Orgao();
		Administracao administracao = new Administracao();
		Consignataria consignataria = new Consignataria();
		
		
		// URL object from API endpoint:
		URL url = new URL(ENDPOINT + "/api/v2/companies");
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
     	//Body:
	    //Building a JSON
	    JSONObject jsonObject = new JSONObject();
	    
	    int typeOfObject = 0; //1 - Averbadora, 2 - Orgao, 3 - Administracao, 4 - Consignataria
	    if(object instanceof Averbadora) {
	    	averbadora = (Averbadora) object;
	    	jsonObject.put("name", averbadora.getCnpj());
		    jsonObject.put("description", 
		    		"Código: "+averbadora.getCodigo()+" "
		    		+"Nome: "+averbadora.getNome()+" "
		    		+"Órgão: "+averbadora.getOrgao().getNome());
		    
		    typeOfObject = 1;
	    } else if(object instanceof Orgao) {
	    	orgao = (Orgao) object;
	    	jsonObject.put("name", orgao.getCnpj());
		    jsonObject.put("description", 
		    		"sigla: "+orgao.getSigla()+" "
		    		+"email: "+orgao.getEmail());
		    
		    typeOfObject = 2;
	    } else if (object instanceof Administracao) {
	    	administracao = (Administracao) object;
	    	jsonObject.put("name", administracao.getDocumento());
		    jsonObject.put("description", 
		    		"documento: "+administracao.getDocumento()+" "
		    		+"email: "+administracao.getEmail());
		    
		    typeOfObject = 3;
	    } else if(object instanceof Consignataria) {
	    	consignataria = (Consignataria) object;
	    	jsonObject.put("name", consignataria.getCnpj());
		    jsonObject.put("description", 
		    		"cnpj: "+consignataria.getCnpj()+" "
		    		+"email: "+consignataria.getEmail());
		    
		    typeOfObject = 4;
	    } else {
	    	return false;
	    }
	    
	    
	    final String jsonBody = jsonObject.toString();
	    HttpEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset(Charset.forName("utf-8")));
	    reqBuilder.setEntity(entity);
	    
	    //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
        
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
            return false;
        } else if(response_status==201) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Entiry Creation Successfull");
                
                //Creating JSONObject for the response string
                JSONObject response_json = new JSONObject(sb.toString());
                System.out.println("Entiry ID: " + response_json.get("id"));
//                System.out.println("Location : " + response.getFirstHeader("location").getValue());
            
                if(typeOfObject == 1) {
                	averbadora.setItsmID(response_json.get("id").toString());
                	averbadoraRepository.save(averbadora);
                } else if(typeOfObject == 2) {
                	orgao.setItsmID(response_json.get("id").toString());
                	orgaoRepository.save(orgao);
                } else if(typeOfObject == 3) {
                	administracao.setItsmID(response_json.get("id").toString());
                	administracaoRepository.save(administracao);
                } else if(typeOfObject == 4) {
                	consignataria.setItsmID(response_json.get("id").toString());
                	consignatariaRepository.save(consignataria);
                } else {
                	return false;
                }
                
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
        
        return true;
        
	}
	
	
	public Usuario createUsuario(Usuario usuario) throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.post();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		URL url = new URL(CONTACT);
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        //Body:
        //Building a JSON
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", usuario.getNome());
        jsonObject.put("email", usuario.getEmail());
        jsonObject.put("phone", "("+usuario.getDdd()+")"+" "+usuario.getCelular());
        jsonObject.put("description", "Matricula: "+usuario.getMatricula()+" Documento: "+usuario.getDocumento()+" Pefil :"+usuario.getPerfil().getNome()+" - ID_INSTITUICAO: "+usuario.getEntidade());
//        jsonObject.put("unique_external_id", usuario.getDocumento());
        
        
        Perfil perfil = usuario.getPerfil();
        if(perfil.getId() == TipoPerfil.AVERBADORA) {
        	
        	Averbadora averbadora = averbadoraService.findAverbadoraById(usuario.getEntidade());
        	
        	if(averbadora != null) {
        		if(averbadora.getItsmID() != null) {
            		jsonObject.put("company_id", Long.parseLong(averbadora.getItsmID()));
            	}
        	}
        	
        	
        } else if (perfil.getId() == TipoPerfil.ORGAO) {
        	
        	Orgao orgao = orgaoService.findOrgaoById(usuario.getEntidade());
        	
        	if(orgao != null) {
        		if(orgao.getItsmID()!=null) {
            		jsonObject.put("company_id", Long.parseLong(orgao.getItsmID()));
            	}
        	}
        	
        } else if (perfil.getId() == TipoPerfil.ADMINISTRADOR) {
        	
        	Administracao administracao = administracaoService.findAdministracaoById(usuario.getEntidade());
        	
        	if(administracao != null) {
        		if(administracao.getItsmID() != null) {
            		jsonObject.put("company_id", Long.parseLong(administracao.getItsmID()));
            	}
        	}
        	
        } else if(perfil.getId() == TipoPerfil.CONSIGNATARIA) {
        	Consignataria consignataria = consignatariaService.findConsignatariaById(usuario.getEntidade());
        	
        	if(consignataria != null) {
        		if(consignataria.getItsmID() != null) {
            		jsonObject.put("company_id", Long.parseLong(consignataria.getItsmID()));
            	}
        	}
        	
        }
        
        final String jsonBody = jsonObject.toString();
        HttpEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset(Charset.forName("utf-8")));
        reqBuilder.setEntity(entity);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
		
        Usuario usuarioSalvo = null;
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
        
//            if(response_body.toLowerCase().contains("unique")) {
//            	Usuario usuarioExistente = saveUserByEmail(usuario.getEmail());
//            	
//            	if(usuarioExistente == null) {
//            		System.out.println("Usuario não criado");
//            	} else {
//            		System.out.println("Usuario updated");
//            	}
//            }
        
        } else if(response_status==201) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Usuario Creation Successfull");
                
                //Creating JSONObject for the response string
                JSONObject response_json = new JSONObject(sb.toString());
                System.out.println("User ID: " + response_json.get("id"));
//                System.out.println("Location : " + response.getFirstHeader("location").getValue());
            
                usuario.setItsmID(response_json.get("id").toString());
                usuarioSalvo = usuarioRepository.save(usuario);
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
		
        return usuarioSalvo;
	}
	
	public boolean deleteEntity(Object object) throws IOException, URISyntaxException {
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.delete();
		rcBuilder = RequestConfig.custom();
		
		Averbadora averbadora = new Averbadora();
		Orgao orgao = new Orgao();
		Administracao administracao = new Administracao();
		Consignataria consignataria = new Consignataria();
		
		JSONObject jsonObject = new JSONObject();
	    
	    int typeOfObject = 0; //1 - Averbadora, 2 - Orgao, 3 - Administracao, 4 - Consignataria
	    if(object instanceof Averbadora) {
	    	averbadora = (Averbadora) object;
	    	
	    	if(averbadora.getItsmID() == null) {
	    		return false;
	    	}
		    
		    typeOfObject = 1;
	    } else if(object instanceof Orgao) {
	    	orgao = (Orgao) object;
		    
	    	if(orgao.getItsmID() == null) {
	    		return false;
	    	}
	    	
		    typeOfObject = 2;
	    } else if (object instanceof Administracao) {
	    	administracao = (Administracao) object;
		    
	    	if(administracao.getItsmID() == null) {
	    		return false;
	    	}
	    	
		    typeOfObject = 3;
	    } else if(object instanceof Consignataria) {
	    	consignataria = (Consignataria) object;
		    
	    	if(consignataria.getItsmID() == null) {
	    		return false;
	    	}
	    	
		    typeOfObject = 4;
	    } else {
	    	return false;
	    }
	    
	    URL url = null;
		
	    if(typeOfObject == 1) {
	    	url = new URL(COMPANY+"/"+averbadora.getItsmID());
	    } else if(typeOfObject == 2) {
	    	url = new URL(COMPANY+"/"+orgao.getItsmID());
	    } else if(typeOfObject == 3) {
	    	url = new URL(COMPANY+"/"+administracao.getItsmID());
	    } else  if(typeOfObject == 4) {
	    	url = new URL(COMPANY+"/"+consignataria.getItsmID());
	    } else {
	    	return false;
	    }
	    
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        
        hccContext.setAuthCache(authCache);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        int response_status = response.getStatusLine().getStatusCode();
        
        if(response_status > 400) {
            return false;
        } else if(response_status==201) {
        	return true;
        }
        
        return true;
	}
	
	public boolean deleteUsuario(Usuario usuario) throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.delete();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		if(usuario.getItsmID()==null) {
			return false;
		}
		
		URL url = new URL(CONTACT+"/"+usuario.getItsmID()+"/hard_delete?force=true");
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
		
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        
        hccContext.setAuthCache(authCache);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        int response_status = response.getStatusLine().getStatusCode();
        
        if(response_status > 400) {
            return false;
        } else if(response_status==201) {
        	return true;
        }
        
        return true;
        
	}
	
	public boolean updateEntity(Object object) throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.put();
		rcBuilder = RequestConfig.custom();
		
		Averbadora averbadora = new Averbadora();
		Orgao orgao = new Orgao();
		Administracao administracao = new Administracao();
		Consignataria consignataria = new Consignataria();
	    
		
		JSONObject jsonObject = new JSONObject();
		
	    int typeOfObject = 0; //1 - Averbadora, 2 - Orgao, 3 - Administracao, 4 - Consignataria
	    if(object instanceof Averbadora) {
	    	averbadora = (Averbadora) object;
	    	
	    	if(averbadora.getItsmID() == null) {
	    		return false;
	    	}
	    	
	    	jsonObject.put("name", averbadora.getNome());
		    jsonObject.put("description", 
		    		"Código: "+averbadora.getCodigo()+" "
		    		+"Nome: "+averbadora.getNome()+" "
		    		+"Órgão: "+averbadora.getOrgao().getNome());
		    
		    typeOfObject = 1;
	    } else if(object instanceof Orgao) {
	    	orgao = (Orgao) object;
		    
	    	if(orgao.getItsmID() == null) {
	    		return false;
	    	}
	    	
	    	jsonObject.put("name", orgao.getNome());
		    jsonObject.put("description", 
		    		"sigla: "+orgao.getSigla()+" "
		    		+"email: "+orgao.getEmail());
	    	
		    typeOfObject = 2;
	    } else if (object instanceof Administracao) {
	    	administracao = (Administracao) object;
		    
	    	if(administracao.getItsmID() == null) {
	    		return false;
	    	}
	    	
	    	jsonObject.put("name", administracao.getNome());
		    jsonObject.put("description", 
		    		"documento: "+administracao.getDocumento()+" "
		    		+"email: "+administracao.getEmail());
	    	
		    typeOfObject = 3;
	    } else if(object instanceof Consignataria) {
	    	consignataria = (Consignataria) object;
		    
	    	if(consignataria.getItsmID() == null) {
	    		return false;
	    	}
	    	
	    	jsonObject.put("name", consignataria.getNome());
		    jsonObject.put("description", 
		    		"cnpj: "+consignataria.getCnpj()+" "
		    		+"email: "+consignataria.getEmail());
	    	
		    typeOfObject = 4;
	    } else {
	    	return false;
	    }
	    
	    URL url = null;
		
	    if(typeOfObject == 1) {
	    	url = new URL(COMPANY+"/"+averbadora.getItsmID());
	    } else if(typeOfObject == 2) {
	    	url = new URL(COMPANY+"/"+orgao.getItsmID());
	    } else if(typeOfObject == 3) {
	    	url = new URL(COMPANY+"/"+administracao.getItsmID());
	    } else  if(typeOfObject == 4) {
	    	url = new URL(COMPANY+"/"+consignataria.getItsmID());
	    } else {
	    	return false;
	    }
		
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        final String jsonBody = jsonObject.toString();
        HttpEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset(Charset.forName("utf-8")));
        reqBuilder.setEntity(entity);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
        
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
        } else if(response_status==200) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Instituicao Updated Successfull");
                
                //Creating JSONObject for the response string
                JSONObject response_json = new JSONObject(sb.toString());
                System.out.println("Instituicao ID: " + response_json.get("id"));
                //System.out.println("Location : " + response.getFirstHeader("location").getValue());
            
                if(typeOfObject == 1) {
                	averbadora.setItsmID(response_json.get("id").toString());
                	averbadoraService.averbadoraRepository.save(averbadora);
                } else if(typeOfObject == 2) {
                	orgao.setItsmID(response_json.get("id").toString());
                	orgaoService.orgaoRepository.save(orgao);
                } else if(typeOfObject == 3) {
                	administracao.setItsmID(response_json.get("id").toString());
                	administracaoService.administracaoRepository.save(administracao);
                } else if(typeOfObject == 4) {
                	consignataria.setItsmID(response_json.get("id").toString());
                	consignatariaService.consignatariaRepository.save(consignataria);
                } else {
                	return false;
                }
                
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
        
        return true;
        
	}
	
	public boolean updateUsuario(Usuario usuario) throws Exception {
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.put();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		URL url = new URL(CONTACT+"/"+usuario.getItsmID());
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        //Body:
        //Building a JSON
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", usuario.getNome());
        jsonObject.put("email", usuario.getEmail());
        jsonObject.put("phone", "("+usuario.getDdd()+")"+" "+usuario.getCelular());
        jsonObject.put("description", "Matricula: "+usuario.getMatricula()+" Documento: "+usuario.getDocumento()+" Pefil :"+usuario.getPerfil().getNome()+" - ID_INSTITUICAO: "+usuario.getEntidade());

        final String jsonBody = jsonObject.toString();
        HttpEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset(Charset.forName("utf-8")));
        reqBuilder.setEntity(entity);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
		
        Usuario usuarioSalvo = null;
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
        } else if(response_status==201) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Ticket Creation Successfull");
                
                //Creating JSONObject for the response string
                JSONObject response_json = new JSONObject(sb.toString());
                System.out.println("Ticket ID: " + response_json.get("id"));
//                System.out.println("Location : " + response.getFirstHeader("location").getValue());
            
                usuario.setItsmID(response_json.get("id").toString());
                usuarioSalvo = usuarioService.edit(usuario).getBody();
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
		
        return true;
	}
	
	public boolean createTicket(TicketDTO ticket) throws IOException, URISyntaxException {
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.post();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		URL url = new URL(TICKET);
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        //Body:
        MultipartEntityBuilder meb = MultipartEntityBuilder.create();
        meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        
        if(ticket.getRequest_id() != null) {
        	meb.addTextBody("requester_id", ticket.getRequest_id());
        } else {
        	meb.addTextBody("requester_id", "43009131164");
        }
        if(ticket.getCompany_id() != null) {
        	meb.addTextBody("company_id", ticket.getCompany_id());
        }
        if(ticket.getEmail() != null) {
        	meb.addTextBody("email", ticket.getEmail());
        }
        if(ticket.getPhone() != null) {
        	meb.addTextBody("phone", ticket.getPhone());
        }
        
        String description = ticket.getDescription();
        
        for(File file : ticket.getFiles()) {
        	meb.addBinaryBody("attachments[]", file
        		, ContentType.TEXT_PLAIN.withCharset("utf-8"), file.getName());
        }
        
        if(ticket.getException() != null) {
        	StringWriter errors = new StringWriter();
        	ticket.getException().printStackTrace(new PrintWriter(errors));
        	description = description + " \r\n " + ticket.getException().getCause()
        			+ " \r\n " +
        			ticket.getException().getMessage()
        			+ " \r\n " +
        			errors.toString();
        	
        }
        
        ticket.setException(null);
        
        meb.addTextBody("subject", ticket.getSubject());
        meb.addTextBody("status", "2");
        meb.addTextBody("description", description);
        meb.addTextBody("priority", Integer.toString(ticket.getPriority()));
        meb.addTextBody("type", ticket.getType());
        meb.addTextBody("responder_id", RESPONDER_ID);
        
        
        for(File file : ticket.getFiles()) {
        	meb.addBinaryBody("attachments[]", file
        		, ContentType.TEXT_PLAIN.withCharset("utf-8"), file.getName());
        }
        reqBuilder.setEntity(meb.build());
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
        
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
            return false;
        } else if(response_status==201) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Ticket Creation Successfull");                
                
                //Creating JSONObject for the response string
                JSONObject response_json = new JSONObject(sb.toString());
                System.out.println("Ticket ID: " + response_json.get("id"));
//                System.out.println("Location : " + response.getFirstHeader("location").getValue());
                
                try {
        			LogAcao logAcao = funcoes.logAcao(Long.parseLong(response_json.get("id").toString()), getDescricaoAbrir(), funcoes.getLoggedUser());
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
                
                for(int i=0; i < ticket.getFiles().size();i++) {
                	try {
            			LogAcao logAcao = funcoes.logAcao(Long.parseLong(response_json.get("id").toString()), getDescricaoAnexarImagem(), funcoes.getLoggedUser());
            			logTicket(null, ticket, logAcao);
                	} catch (Exception e) {
            			e.printStackTrace();
            		}
                }
                
                return true;
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
        
        return false;
	}
	
	public boolean updateTicket(TicketDTO ticket) throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		
		rcBuilder = RequestConfig.custom();
		
		TicketDTO previous = getTicket(ticket.getId());
		reqBuilder = RequestBuilder.put();
		
		// URL object from API endpoint:
		URL url = new URL(TICKET+"/"+ticket.getId());
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        //Body:
        MultipartEntityBuilder meb = MultipartEntityBuilder.create();
        meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        
        if(ticket.getRequest_id() != null) {
        	meb.addTextBody("requester_id", ticket.getRequest_id());
        }
//        if(ticket.getCompany_id() != null) {
//        	meb.addTextBody("company_id", ticket.getCompany_id());
//        }
        if(ticket.getEmail() != null) {
        	meb.addTextBody("email", ticket.getEmail());
        }
        if(ticket.getPhone() != null) {
        	meb.addTextBody("phone", ticket.getPhone());
        }
        
        meb.addTextBody("subject", ticket.getSubject());
        if(ticket.getStatus() != null) {
        	meb.addTextBody("status", ticket.getStatus());
        } else {
        	meb.addTextBody("status", "2");
        }
        meb.addTextBody("description", ticket.getDescription());
        meb.addTextBody("priority", Integer.toString(ticket.getPriority()));
        meb.addTextBody("type", ticket.getType());
        meb.addTextBody("responder_id", RESPONDER_ID);
        
        for(File file : ticket.getFiles()) {
        	meb.addBinaryBody("attachments[]", file
        		, ContentType.TEXT_PLAIN.withCharset("utf-8"), file.getName());
        }
        reqBuilder.setEntity(meb.build());
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
        
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
            return false;
        } else if(response_status==201 || response_status==200) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Ticket Updated Successfull");
                
                //Creating JSONObject for the response string
                JSONObject response_json = new JSONObject(sb.toString());
                System.out.println("Ticket ID: " + response_json.get("id"));
//                System.out.println("Location : " + response.getFirstHeader("location").getValue());
                
                try {
        			LogAcao logAcao = funcoes.logAcao(Long.parseLong(response_json.get("id").toString()), getDescricaoEditar(), funcoes.getLoggedUser());
        			logTicket(previous, ticket, logAcao);
                } catch (Exception e) {
        			e.printStackTrace();
        		}
                
                return true;
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
        
        return false;
	}
	
	public boolean deleteTicket(Long id) throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.delete();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		if(id==null) {
			return false;
		}
		
		URL url = new URL(TICKET+"/"+id);
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        
        hccContext.setAuthCache(authCache);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        int response_status = response.getStatusLine().getStatusCode();
        
        if(response_status > 400) {
            return false;
        } else if(response_status==201) {
        	return true;
        }
        
        return true;
	}
	
	
	public JSONArray getAllTickets() throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.get();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		URL url = new URL(TICKET);
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
        
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
        } else if(response_status==200) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Tickets Found");
                
                //Creating JSONObject for the response string
                JSONArray response_json = new JSONArray(sb.toString());
                return response_json;
            
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
        
        return new JSONArray();
	}
	
	public Page<TicketDTO> getFilteredTickets(Pageable pageable, TicketDTO ticket) throws IOException, URISyntaxException {
		
		String query = "";
		
		StringBuilder queryBuilder = new StringBuilder();
		
		
		if(ticket.getType() != null) {
			if(ticket.getStatus() != null) {
				
				queryBuilder.append("?query=%22type%3A"+ticket.getType()+"%20AND%20status%3A"+ticket.getStatus()+"%22");
				
				//query = query + "?query=\"type:"+ticket.getType()+"%20AND%20status:"+ticket.getStatus()+"\"";
			} else {
				
				queryBuilder.append("?query=%22type%3A"+ticket.getType()+"%22");
				//query = query + "?query=\"type:"+ticket.getType()+"\"";
			}
		} else if(ticket.getStatus() != null) {
			//query = query + "?query=\"type:"+ticket.getStatus()+"\"";
			
			queryBuilder.append("?query=%22status:"+ticket.getStatus()+"%22");
			
		}
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.get();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		URL url = null;
		if(ticket.getType() == null && ticket.getStatus() == null) {
			url = new URL(TICKET);
		} else {
			url = new URL(TICKET_SEARCH+queryBuilder.toString());
		}
		
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        
        System.out.println(url.toURI().toString());
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
        
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
            throw new GenericException("Erro", "Erro de comunicação com o serviço de ticket.");
        } else if(response_status==200) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                System.out.println("Ticket Found");
                
                //Creating JSONObject for the response string
                JSONArray response_json = new JSONArray("[]");
                if(!sb.toString().contains("results")) {
                	response_json = new JSONArray(sb.toString());
                } else {
                	JSONObject jsonObject = new JSONObject(sb.toString());
                	response_json = jsonObject.getJSONArray("results");
                }
                
                
                List<TicketDTO> tickets = getTicketsFromJson(response_json, ticket);
                
                int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
        		int end = (start + pageable.getPageSize()) > tickets.size() ? tickets.size() : (start + pageable.getPageSize());
        		
        		return new PageImpl<TicketDTO>(tickets.subList(start, end), pageable, tickets.size());
            
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
        
        return null;
	}
	
	public List<TicketDTO> getTicketsFromJson(JSONArray jsonArray, TicketDTO ticketForSearch){
		
		
		List<TicketDTO> tickets = new ArrayList<TicketDTO>();
		
		if(jsonArray.length() > 0) {
			
			for(int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);

				TicketDTO ticketDTO = new TicketDTO();
				
				if(jsonObject.has("id")) {
					ticketDTO.setId(jsonObject.get("id").toString());
				}
				
				if(jsonObject.has("requester_id")) {
					ticketDTO.setRequest_id(jsonObject.get("requester_id").toString());
					
					Usuario usuario = usuarioService.findByITSMID(jsonObject.get("requester_id").toString());
					
					if(usuario != null) {
						
						if(!funcoes.getLoggedUser().getPerfil().getId().equals(TipoPerfil.ADMINISTRADOR)
							&& !funcoes.getLoggedUser().getPerfil().getId().equals(TipoPerfil.SUPREMO)) {
							
							if(!usuario.getId().equals(funcoes.getLoggedUser().getId())) {
								continue;
							}
							
						}
						
						
						ticketDTO.setNomeUsuario(usuario.getNome());
						ticketDTO.setCpf(usuario.getDocumento());
					} else {
						continue;
					}
					
				}
				
				if(ticketForSearch.getNomeUsuario() != null && !ticketForSearch.getNomeUsuario().isEmpty()) {
					if(!ticketDTO.getNomeUsuario().contains(ticketForSearch.getNomeUsuario())) {
						continue;
					}
				}
				
				if(jsonObject.has("company_id")) {
					ticketDTO.setCompany_id(jsonObject.get("company_id").toString());
				}
				
				if(jsonObject.has("email")) {
					ticketDTO.setEmail(jsonObject.get("email").toString());
				}
				
				if(jsonObject.has("phone")) {
					ticketDTO.setPhone(jsonObject.get("phone").toString());
				}
				
				if(jsonObject.has("subject")) {
					ticketDTO.setSubject(jsonObject.get("subject").toString());
					
					if(ticketForSearch.getSubject() != null && !ticketForSearch.getSubject().isEmpty()) {
						if(!ticketDTO.getSubject().contains(ticketForSearch.getSubject())) {
							continue;
						}
					}
					
				}
				
				if(jsonObject.has("description")) {
					ticketDTO.setDescription(jsonObject.get("description").toString());
				}
				
				if(jsonObject.has("created_at")) {
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					
					
					ticketDTO.setDataAbertura(formatDateString(jsonObject.get("created_at").toString()));
					
					if(ticketForSearch.getDataAbertura() != null && !ticketForSearch.getDataAbertura().isEmpty()) {
						String[] dataForSearch = ticketDTO.getDataAbertura().split(" ");
						if(!dataForSearch[0].equals(ticketForSearch.getDataAbertura())) {
							continue;
						}
					}
					
				}
				
				if(jsonObject.has("priority")) {
					ticketDTO.setPriority(jsonObject.getInt("priority"));
					
					
					if(ticketDTO.getPriority() == 1) {
						ticketDTO.setPrioridade("BAIXA");
					} else if(ticketDTO.getPriority() == 2) {
						ticketDTO.setPrioridade("MÉDIA");
					} else if(ticketDTO.getPriority() == 3) {
						ticketDTO.setPrioridade("ALTA");
					} else if(ticketDTO.getPriority() == 4) {
						ticketDTO.setPrioridade("URGENTE");
					}
					
					if(ticketForSearch.getPriority() != 0) {
						if(ticketDTO.getPriority() != ticketForSearch.getPriority()) {
							continue;
						}
					}
				}
				
				if(jsonObject.has("type")) {
					ticketDTO.setType(jsonObject.get("type").toString());
					
					if(ticketDTO.getType().equals("Question")) {
						ticketDTO.setTipo("QUESTÃO");
					} else if(ticketDTO.getType().equals("Incident")) {
						ticketDTO.setTipo("INCIDENTE");
					} else if(ticketDTO.getType().equals("Problem")) {
						ticketDTO.setTipo("PROBLEMA");
					} else if(ticketDTO.getType().equals("Feature Request")) {
						ticketDTO.setTipo("SOLICITAÇÃO DE RECURSO");
					} else if(ticketDTO.getType().equals("Refund")) {
						ticketDTO.setTipo("REEMBOLSO");
					}
					
				}
				
				if(jsonObject.has("status")) {
					
					ticketDTO.setStatus(jsonObject.get("status").toString());
					
					if(ticketDTO.getStatus().equals("2")) {
						ticketDTO.setStatus("ABERTO");
					} else if(ticketDTO.getStatus().equals("3")) {
						ticketDTO.setStatus("PENDENTE");
					} else if(ticketDTO.getStatus().equals("4")) {
						ticketDTO.setStatus("RESOLVIDO");
					}
					
					if(ticketDTO.getStatus().equals("5")) {
						ticketDTO.setStatus("FECHADO");
						ticketDTO.setDataFechamento(jsonObject.get("updated_at").toString());
						
						if(ticketForSearch.getDataFechamento() != null && !ticketForSearch.getDataFechamento().isEmpty()) {
							String[] dataForSearch = ticketDTO.getDataFechamento().split(" ");
							if(!dataForSearch[0].equals(ticketForSearch.getDataFechamento())) {
								continue;
							}
						}
						
					} else {
						ticketDTO.setDataFechamento("NÂO FECHADO");
					}
				}
				
				if(ticketDTO.getId() != null) {
					tickets.add(ticketDTO);
				}
				
			}
			
		}
		
		return tickets;
		
	}
	
	private String formatDateString(String data) {
		
		String[] splitedStr = data.split("T");
		String[] dataSplt = splitedStr[0].split("-");
		
		String dia = dataSplt[2];
		String mes = dataSplt[1];
		String ano = dataSplt[0];
		
		String time = splitedStr[1].substring(0, splitedStr[1].length() - 1);
		
		
		
		String dateFormated = dia + "/" + mes + "/" + ano + " " + time;
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		TimeZone timezone = TimeZone.getTimeZone("America/Fortaleza");
		sdf.setTimeZone(timezone);
		Date date = null;
		try {
			date = sdf.parse(dateFormated);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(date != null) {
			return sdf.format(date);
		} else {
			return dateFormated;
		}
	}
	
	public TicketDTO getTicket(String id) throws IOException, URISyntaxException {
		
		//Inicializando variáveis
		hcBuilder = HttpClientBuilder.create();
		reqBuilder = RequestBuilder.get();
		rcBuilder = RequestConfig.custom();
		
		// URL object from API endpoint:
		URL url = new URL(TICKET+"/"+id);
		final String urlHost = url.getHost();
        final int urlPort = url.getPort();
        final String urlProtocol = url.getProtocol();
        reqBuilder.setUri(url.toURI());
        
        // Authentication:
        List<String> authPrefs = new ArrayList<>();
        authPrefs.add(AuthSchemes.BASIC);
        rcBuilder.setTargetPreferredAuthSchemes(authPrefs);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(urlHost, urlPort, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(API_TOKEN, "X"));
        hcBuilder.setDefaultCredentialsProvider(credsProvider);
        AuthCache authCache = new BasicAuthCache();
        AuthSchemeBase authScheme = new BasicScheme();
        authCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authScheme);
        HttpClientContext hccContext = HttpClientContext.create();
        hccContext.setAuthCache(authCache);
        
        //Execute:
        RequestConfig rc = rcBuilder.build();
        reqBuilder.setConfig(rc);
        
        HttpClient hc = hcBuilder.build();
        HttpUriRequest req = reqBuilder.build();
        HttpResponse response = hc.execute(req, hccContext);
        
        //Print out
        HttpEntity body = response.getEntity();
        InputStream is = body.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
        String line;
        StringBuilder sb = new StringBuilder();
        
        while((line=br.readLine())!=null) {
            sb.append(line);
        }
        
        int response_status = response.getStatusLine().getStatusCode();
        String response_body = sb.toString();
        
        System.out.println("Response Status: "+ response_status);
        System.out.println("Body:\n");
        System.out.println(response_body);
        
        if(response_status > 400) {
            System.out.println("X-Request-Id: " + response.getFirstHeader("x-request-id").getValue());
        } else if(response_status==200) {
            //For creation response_status is 201 where are as for other actions it is 200
            try{
                
            	TicketDTO ticketDTO = new TicketDTO();
            	
                //Creating JSONObject for the response string
                JSONObject response_json = new JSONObject(sb.toString());
                
                if(response_json.has("id")) {
                	ticketDTO.setId(response_json.get("id").toString());
                }
                
                if(response_json.has("requester_id")) {
                	ticketDTO.setRequest_id(response_json.get("requester_id").toString());
                }
                
                if(response_json.has("company_id")) {
                	ticketDTO.setCompany_id(response_json.get("company_id").toString());
                }

                if(response_json.has("email")) {
                	ticketDTO.setEmail(response_json.get("email").toString());
                }

                if(response_json.has("phone")) {
                	ticketDTO.setPhone(response_json.get("phone").toString());
                }

                if(response_json.has("subject")) {
                	ticketDTO.setSubject(response_json.get("subject").toString());
                }

                if(response_json.has("description")) {
                	ticketDTO.setDescription(response_json.get("description").toString());
                }

                if(response_json.has("priority")) {
                	ticketDTO.setPriority(response_json.getInt("priority"));
                }
                
                if(response_json.has("status")) {
                	ticketDTO.setStatus(response_json.get("status").toString());
                }

                if(response_json.has("type")) {
                	ticketDTO.setType(response_json.get("type").toString());
                }
                
                if(response_json.has("created_at")) {
                	ticketDTO.setDataAbertura(formatDate(response_json.get("created_at").toString()));
                } else {
                	ticketDTO.setDataAbertura("Sem data");
                }
                
                if(response_json.has("status")) {
                	if(Integer.parseInt(response_json.get("status").toString()) == 5) {
                		if(response_json.has("updated_at")) {
                			ticketDTO.setDataFechamento(formatDate(response_json.get("updated_at").toString()));
                		} else {
                			ticketDTO.setDataFechamento("Sem Data");
                		}
                	} else {
                		ticketDTO.setDataFechamento("Ticket ainda não fechado");
                	}
                	
                }
                
                
                if(response_json.has("attachments")) {
                	
                	JSONArray anexosArray = response_json.getJSONArray("attachments");
                	
                	if(anexosArray.length() > 0) {
                		ticketDTO.setFiles(new ArrayList<File>());
                		JSONObject object;
//                		TicketAnexoDTO anexo;
//                		ArrayList<TicketAnexoDTO> anexos = new ArrayList<TicketAnexoDTO>();
                		
                		for(int i = 0; i < anexosArray.length(); i++) {
//                			anexo = new TicketAnexoDTO();
                			
                			object = anexosArray.getJSONObject(i);
                			
//                			if(object.has("id")) {
//                				anexo.setId(object.get("id").toString());
//                			}
//                			
                			
                			if(object.has("attachment_url")) {
                				
                				URL urlFile = new URL(object.get("attachment_url").toString());
                				InputStream inputStream = url.openStream();
                				
                				String prefix = generatePrefix();
                				String suffix = "";
                				
                				if(object.has("name")) {
                					String[] name = object.get("name").toString().split(".");
                					suffix = name[name.length - 1];
                    			}
                				
                				File tempFile = File.createTempFile(prefix, suffix);
                				FileOutputStream out = new FileOutputStream(tempFile);
                				IOUtils.copy(inputStream, out);
//                				anexo.setUrl(object.get("attachment_url").toString());
                				
                				ticketDTO.getFiles().add(tempFile);
                				
                			}
//                			anexo.setTicket(ticketDTO);
//                			anexos.add(anexo);
                		}
                	}
                }
                
                return ticketDTO;
            
            }
            catch(JSONException e){
                System.out.println("Error in JSON Parsing\n :"+ e);
            }
        }
        
        return null;
	}
	
	public String generatePrefix() {
		Random rand = new Random();
		
		int number = rand.nextInt(9999);
		
		return Integer.toString(number);
	}

	public String formatDate(String date) {
		String[] dateAndTime = date.split("T");
		
		String[] onlyDate = dateAndTime[0].split("-");
		String day = onlyDate[2];
		String month = onlyDate[1];
		String year = onlyDate[0];
		
		String time = dateAndTime[1].substring(0, dateAndTime[1].length()-1);
		
		return day+"/"+month+"/"+year+" "+time;
	}
	
	public Long getDescricaoAbrir() {
		return DescricaoLogAcaoHelper.ABRIR_TICKET;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_TICKET;
	}

	public Long getDescricaoFechar() {
		return DescricaoLogAcaoHelper.FECHAR_TICKET;
	}
	
	public Long getDescricaoAnexarImagem() {
		return DescricaoLogAcaoHelper.ANEXAR_IMAGEM_TICKET;
	}
	
	public Long getDescricaoReparar() {
		return DescricaoLogAcaoHelper.REPARAR_TICKET;
	}
	
	public TicketDTO parseJSON(JSONObject jsonObject) {
		TicketDTO ticketDTO = new TicketDTO();
		
		if(jsonObject.has("id")) {
			ticketDTO.setId(jsonObject.get("id").toString());
		}
		
		if(jsonObject.has("subject")) {
			ticketDTO.setSubject(jsonObject.get("subject").toString());
		}
		
		if(jsonObject.has("description")) {
			ticketDTO.setDescription(jsonObject.get("description").toString());
		}
		
		if(jsonObject.has("type")) {
			ticketDTO.setType(jsonObject.get("type").toString());
		}
		
		if(jsonObject.has("priority")) {
			ticketDTO.setPriority(jsonObject.getInt("priority"));
		}
		
		return ticketDTO;
	}
	
	public void logTicket(TicketDTO previous, TicketDTO current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new TicketDTO();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");

		logUtil.withEntidade("Ticket");
		
		logs.add(logUtil.fromValues("request_id", 
				previous.getRequest_id() == null ? "-" : previous.getRequest_id(),
				current.getRequest_id() == null ? "-" : current.getRequest_id(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("company_id", 
				previous.getCompany_id() == null ? "-" : previous.getCompany_id(),
				current.getCompany_id() == null ? "-" : current.getCompany_id(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("email", 
				previous.getEmail() == null ? "-" : previous.getEmail(),
				current.getEmail() == null ? "-" : current.getEmail(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("phone", 
				previous.getPhone() == null ? "-" : previous.getPhone(),
				current.getPhone() == null ? "-" : current.getPhone(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("subject", 
				previous.getSubject() == null ? "-" : previous.getSubject(),
				current.getSubject() == null ? "-" : current.getSubject(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("description", 
				previous.getDescription() == null ? "-" : previous.getDescription(),
				current.getDescription() == null ? "-" : current.getDescription(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("dataAbertura", 
				previous.getDataAbertura() == null ? "-" : previous.getDataAbertura(),
				current.getDataAbertura() == null ? "-" : current.getDataAbertura(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("dataFechamento", 
				previous.getDataFechamento() == null ? "-" : previous.getDataFechamento(),
				current.getDataFechamento() == null ? "-" : current.getDataFechamento(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("type", 
				previous.getType() == null ? "-" : previous.getType(),
				current.getType() == null ? "-" : current.getType(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status", 
				previous.getStatus() == null ? "-" : previous.getStatus(),
				current.getStatus() == null ? "-" : current.getStatus(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("cpf", 
				previous.getCpf() == null ? "-" : previous.getCpf(),
				current.getCpf() == null ? "-" : current.getCpf(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("nomeUsuario", 
				previous.getNomeUsuario() == null ? "-" : previous.getNomeUsuario(),
				current.getNomeUsuario() == null ? "-" : current.getNomeUsuario(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("priority", 
				Integer.toString(previous.getPriority()),
				Integer.toString(current.getPriority()),
				datAlteracao));
		
		logs.add(logUtil.fromValues("attachments", 
				previous.getFiles() == null ? "files: 0" : "files: "+previous.getFiles().size(),
				current.getFiles() == null ? "files: 0" : "files: "+current.getFiles().size(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("exception", 
				previous.getException() == null ? "-" : previous.getException().getClass().getSimpleName(),
				current.getException() == null ? "-" : current.getException().getClass().getSimpleName(),
				datAlteracao));
		
		if(logs.isEmpty()) {
			return;
		}
		
		for(Log log : logs) {
			if(log!=null) {
				log.setIdRow(Long.parseLong(current.getId()));
				log.setLogAcao(logAcao);
				logRepository.save(log);
			}
			
		}
		
	}
	
}
