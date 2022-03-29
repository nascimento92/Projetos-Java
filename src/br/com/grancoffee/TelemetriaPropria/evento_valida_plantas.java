package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

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
		update(arg0);		
	}
	
	private void insert(PersistenceEvent arg0) {
		valida(arg0);		
	}
	
	private void update(PersistenceEvent arg0) {
		valida(arg0);
		
		//TODO :: valida CEP
		//TODO :: verificar API https://viacep.com.br/ws/13187148/json/
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
