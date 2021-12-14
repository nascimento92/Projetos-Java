package br.com.gsn.app.gcDigital;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

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

public class evento_criaOS implements EventoProgramavelJava{

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String patrimonio = VO.asString("CODBEM");
		String nome = VO.asString("NOME");
		String telefone = VO.asString("TELEFONE");
		Timestamp dataSolicitacao = VO.asTimestamp("DTSOLICIT");
		String email = VO.asString("EMAIL");
		String descricao = VO.asString("DESCRICAO");
		String tipo = VO.asString("TIPO");
		String enderecoMaquina = null;
		BigDecimal contrato = null;
		String parceiro = null;
		
		if(patrimonio!=null) {
			enderecoMaquina = getEnderecoDaMaquina(patrimonio);
			contrato = getContrato(patrimonio);
			parceiro = getParceiro(patrimonio);
		}
	}
	
	private String getEnderecoDaMaquina(String patrimonio) {
		String end = "";
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT (P.NOMEPLAN||' - '||P.ENDPLAN) AS ENDERECO FROM AD_ENDERECAMENTO T LEFT JOIN AD_PLANTAS P ON (P.NUMCONTRATO=T.NUMCONTRATO AND P.ID=T.ID) WHERE T.CODBEM='"+patrimonio+"' AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				end = contagem.getString("ENDERECO");
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return end;
	}
	
	private BigDecimal getContrato(String patrimonio) {
		BigDecimal contrato = BigDecimal.ZERO;
		try {
			JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
			DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });

			if(VO!=null) {
				contrato = VO.asBigDecimal("NUMCONTRATO");
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return contrato;
	}
	
	private String getParceiro(String patrimonio) {
		String parc = "";
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT (P.CODPARC||' - '||P.NOMEPARC) AS PARCEIRO FROM AD_ENDERECAMENTO T LEFT JOIN TCSCON C ON (C.NUMCONTRATO=T.NUMCONTRATO) LEFT JOIN TGFPAR P ON (P.CODPARC=C.CODPARC) WHERE T.CODBEM='"+patrimonio+"' AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				parc = contagem.getString("PARCEIRO");
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return parc;
	}
}
