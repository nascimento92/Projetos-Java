package br.com.grancoffee.TelemetriaPropria;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.Normalizer;
import org.json.simple.parser.ParseException;
import com.google.gson.JsonObject;
import com.sankhya.util.JsonUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class evento_valida_plantas implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		insert(arg0);		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		insert(arg0);		
	}
	
	private void insert(PersistenceEvent arg0) throws Exception {
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal cidade = VO.asBigDecimal("CODCID");
		BigDecimal bairro = VO.asBigDecimal("CODBAI");
		BigDecimal endereco = VO.asBigDecimal("CODEND");
		BigDecimal cep = VO.asBigDecimal("CEP");
		
		DynamicVO cepNativo = getCEPNativo(cep);
		
		valida(arg0);
		
		if(cepNativo!=null) {
			BigDecimal cidadeN = cepNativo.asBigDecimal("CODCID");
			BigDecimal bairroN = cepNativo.asBigDecimal("CODBAI");
			BigDecimal enderecoN = cepNativo.asBigDecimal("CODEND");
			
			if(cidade==null) {
				VO.setProperty("CODCID", cidadeN);
			}
			
			if(bairro==null) {
				VO.setProperty("CODBAI", bairroN);
			}
			
			if(endereco==null) {
				VO.setProperty("CODEND", enderecoN);
			}
		}else {
			String url = "https://viacep.com.br/ws/"+cep.toString()+"/json/";
			String request = request(url);
	
			JsonObject json = JsonUtils.convertStringToJsonObject(request);
			
			String enderecoA = removerCaracter(json.get("logradouro").toString());
			String bairroA = removerCaracter(json.get("bairro").toString());
			String cidadeA = removerCaracter(json.get("localidade").toString());
			
			BigDecimal codCidadeA = getCidade(cidadeA);
			BigDecimal codEnderecoA = getEndereco(enderecoA);
			BigDecimal codBairroA = getBairro(bairroA);
			
			/* utilizado com GSON (skw não suportou)
			
			JsonObject convertedObject = new Gson().fromJson(request, JsonObject.class);
			
			String enderecoA = removerCaracter(convertedObject.get("logradouro").toString());
			String bairroA = removerCaracter(convertedObject.get("bairro").toString());
			String cidadeA = removerCaracter(convertedObject.get("localidade").toString());

			
			BigDecimal codCidadeA = getCidade(cidadeA);
			BigDecimal codEnderecoA = getEndereco(enderecoA);
			BigDecimal codBairroA = getBairro(bairroA);
			*/
			
			if(cidade==null) {
				if(codCidadeA!=null) {
					VO.setProperty("CODCID", codCidadeA);
				}	
			}
			
			if(bairro==null) {
				if(codBairroA!=null) {
					VO.setProperty("CODBAI", codBairroA);
				}	
			}
			
			if(endereco==null) {
				if(codEnderecoA!=null) {
					VO.setProperty("CODEND", codEnderecoA);
				}	
			}
			
			
		}
			
	}
	
	private String request(String url) throws IOException, ParseException {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder()
				.url(url)
				.get()
				.build();

		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}
	
	private BigDecimal getCidade(String cidade) throws Exception {
		BigDecimal cod = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CODCID FROM TSICID WHERE NOMECID LIKE '%"+cidade+"%' AND ROWNUM=1");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			cod = contagem.getBigDecimal("CODCID");
		}
		
		return cod;
	}
	
	private BigDecimal getEndereco(String endereco) throws Exception {
		BigDecimal cod = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CODEND FROM TSIEND WHERE (UPPER(NOMEEND) LIKE '%"+endereco+"%' OR UPPER(TIPO||' '||NOMEEND) LIKE '%"+endereco+"%') AND ROWNUM=1");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			cod = contagem.getBigDecimal("CODEND");
		}
		
		return cod;
	}
	
	private BigDecimal getBairro(String bairro) throws Exception {
		BigDecimal cod = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CODBAI FROM TSIBAI WHERE NOMEBAI LIKE '%"+bairro+"%' AND ROWNUM=1");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			cod = contagem.getBigDecimal("CODBAI");
		}
		
		return cod;
	}
	
	private String removerCaracter(String valor) {
		
		String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	    String semCaracteresEspeciais=Normalizer.normalize(semAcento, Normalizer.Form.NFD).replaceAll("[(|!?¨*°;:{}$#%^~&'\"\\<>)]", "");
	    String stringFInal = semCaracteresEspeciais.trim().replaceAll("\\s+", " ").toUpperCase();
	   
		return stringFInal;
	}
		
	
	private DynamicVO getCEPNativo(BigDecimal cep) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_CEPCOMM");
		DynamicVO VO = DAO.findOne("CEP=?",new Object[] { cep.toString() });
		return VO;
	}
	
	private void valida(PersistenceEvent arg0) {
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String nome = VO.asString("NOME");
		BigDecimal endereco = VO.asBigDecimal("CODEND");
		String numero = VO.asString("NUMERO");
		String complemento = VO.asString("COMPLEMENTO");
		BigDecimal bairro = VO.asBigDecimal("CODBAI");
		BigDecimal cidade = VO.asBigDecimal("CODCID");
		BigDecimal cep = VO.asBigDecimal("CEP");
		
		boolean valida = false;
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) FROM AD_GCPLANTA WHERE NOME='"+nome+"' AND CODEND="+endereco+" AND NUMERO='"+numero+"' AND COMPLEMENTO='"+complemento+"' AND CODBAI="+bairro+" AND CODCID="+cidade+" AND CEP="+cep);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count > 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		if(valida) {
			throw new Error("<br/><b>ATENÇÃO</b><br/><br/>Endereço já cadastrado!");
		}
	}

}
