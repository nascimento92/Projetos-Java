package br.com.gsn.app.gcDigital;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_faleConosco_enviaEmail implements EventoProgramavelJava{
	
	/**
	 * @author Gabriel
	 * 10/12/21 vs 1.0 Realiza o envio dos e-mails para sac@grancoffee.com.br referente aos motivos de sugestão e reembolso.
	 */
	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
				
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
		start(arg0);	
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		
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
		
		enviarEmail(tipo,dataSolicitacao,parceiro,contrato,nome,telefone, email, patrimonio, enderecoMaquina,descricao);
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
	
	private void enviarEmail(String tipo, Timestamp data, String parceiro, BigDecimal contrato, String nome,String telefone, String email,
			String patrimonio, String enderecoMaquina, String descricao) throws Exception {
		
		String emailDestino = "sac@grancoffee.com.br";
		
		String descTipo="";
		if("1".equals(tipo)) {
			descTipo = "Chamado Técnico";
		}else if ("2".equals(tipo)) {
			descTipo = "Sugestão/Reclamação";
		}else {
			descTipo = "Reembolso";
		}
		
		Timestamp buildPrintableTimestamp = TimeUtils.buildPrintableTimestamp(data.getTime(), "dd/MM/yyyy HH:mm:ss");
		
		try {
			
			String mensagem = 
					"<h2>Nova Solicitação - "+descTipo+"</h2>"+
					"<p><b>Data Solicitação: </b>"+buildPrintableTimestamp+"</p>"+
					"<p><b>Parceiro: </b>"+parceiro+"</p>"+
					"<p><b>Contrato: </b>"+contrato+"</p>"+
					"<p><b>Solicitante: </b>"+nome+"</p>"+
					"<p><b>Telefone: </b>"+telefone+"</p>"+
					"<p><b>E-mail: </b>"+email+"</p>"+
					"<p><b>Patrimônio: </b>"+patrimonio+"</p>"+
					"<p><b>Endereço patrimônio: </b>"+enderecoMaquina+"</p>"+
					"<p><b>Descrição: </b>"+descricao+"</p>"+
					"<br/>"+
					"<p><b>Esta é uma mensagem automática, por gentileza, não respondê-la!</b></p>"+
					"<p>Att,</p>"+
					"<img src=\"https://i.ibb.co/ym9v8Fx/e71416f897.png\" width=\"280\" height=\"80\">";
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODFILA", getUltimoCodigoFila());
			VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("MENSAGEM", mensagem.toCharArray());
			VO.setProperty("TIPOENVIO", "E");
			VO.setProperty("ASSUNTO", new String("Nova Solicitação APP - Reclamação - "+nome));
			VO.setProperty("EMAIL", emailDestino);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));	
			VO.setProperty("CODSMTP", getContaSmtpPrincipal());
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[buildPrintableTimestamp] erro ao enviar e-mail ! patrimonio"+patrimonio+" solicitante "+nome+" data "+data);
		}	
	}
	
	private BigDecimal getContaSmtpPrincipal() throws Exception {
		int count = 1;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODSMTP) AS COD FROM TSISMTP WHERE PADRAO = 'S'");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("COD");
		}

		BigDecimal codigoConta = new BigDecimal(count);

		return codigoConta;
	}
	
	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_abastecimento");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
