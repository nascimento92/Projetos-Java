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
import br.com.sankhya.modelcore.util.DynamicEntityNames;
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
		BigDecimal codparc = null;
		BigDecimal servico = null;
		BigDecimal motivo = null;
		BigDecimal executante = null;
		BigDecimal osModelo = null;
		String problema = "";
		String t = "";
		BigDecimal codprod = null;
		
		if(patrimonio!=null && tipo!=null) {
			
			if(validaSeEhUmaMaquinaValida(patrimonio)) {
				enderecoMaquina = getEnderecoDaMaquina(patrimonio);
				contrato = getContrato(patrimonio);
				parceiro = getParceiro(patrimonio);
				codparc = getCodParc(patrimonio);
						
				if("1".equals(tipo)) { //Chamado Técnico
					servico = new BigDecimal(515315);
					motivo = new BigDecimal(4);
					//TODO: obter o executante baseado na operação da planta do contrato
					executante = getExecutanteChamadoTecnico(patrimonio);
					t = "Chamado Técnico";
					osModelo = new BigDecimal(629042);
					
				}else if("2".equals(tipo)) { //Sugestão/Reclamação
					servico = new BigDecimal(200000);
					motivo = new BigDecimal(106);
					executante = new BigDecimal(2028);
					t = "Sugestão/Reclamação";
					osModelo = new BigDecimal(629042);
					
				}else { //Reembolso
					servico = new BigDecimal(200000);
					motivo = new BigDecimal(8);
					executante = new BigDecimal(54);
					t = "Reembolso";
					osModelo = new BigDecimal(629042);
				}
				
				problema = "Tipo de solicitação: "+t+
						"\nPatrimônio: "+patrimonio+
						"\nData Solicitação: "+TimeUtils.buildPrintableTimestamp(dataSolicitacao.getTime(), "dd/MM/yyyy HH:mm:ss")+
						"\nContrato: "+contrato+
						"\nParceiro: "+parceiro+
						"\nSolicitante: "+nome+
						"\nTelefone: "+telefone+
						"\nE-mail: "+email+
						"\nEndereço da máquina: "+enderecoMaquina+
						"\nSolicitação: "+descricao;

				codprod = getCodprod(patrimonio);
				
				BigDecimal numos = gerarCabecalhoOS(problema,osModelo,patrimonio,contrato,codparc);
				if(numos.intValue()!=0) {
					geraItemOS(numos,osModelo,motivo,servico,executante, codprod, patrimonio);
					VO.setProperty("NUMOS", numos);
				}
			}else {
				VO.setProperty("ERRO", "Máquina "+patrimonio+" inválida!");
			}
	
		}
	}
	
	private boolean validaSeEhUmaMaquinaValida(String patrimonio) {
		boolean valida = false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) AS QTD FROM AD_PATRIMONIO WHERE codbem='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valida;
	}
	
	private BigDecimal gerarCabecalhoOS (String solicitacao, BigDecimal osModelo, String patrimonio, 
			BigDecimal contrato, BigDecimal parceiro){
		
		BigDecimal numos = BigDecimal.ZERO;
		
		String problema = 
						"SOLICITAÇÃO GC DIGITAL"+
						"\n"+solicitacao;
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico", osModelo);
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			Timestamp dataAtual=TimeUtils.getNow();
			BigDecimal usuario = new BigDecimal(815);

			NotaProdVO.setProperty("DHCHAMADA", dataAtual);
			NotaProdVO.setProperty("DTPREVISTA",addDias(dataAtual,7));
			NotaProdVO.setProperty("NUMOS",null); 
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",usuario);
			NotaProdVO.setProperty("CODUSURESP",usuario);
			NotaProdVO.setProperty("DESCRICAO",problema);
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			NotaProdVO.setProperty("AD_CHAMADOTI", "N");
			NotaProdVO.setProperty("CODATEND", usuario);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("AD_TELASAC", "S");
			NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			NotaProdVO.setProperty("CODCONTATO", getMenorContato(parceiro));
			
			NotaProdVO.setProperty("DTFECHAMENTO", null);
			NotaProdVO.setProperty("CODUSUFECH", null);
			
			if(contrato!=null) {
				NotaProdVO.setProperty("NUMCONTRATO", contrato);
			}
			
			if(parceiro!=null) {
				NotaProdVO.setProperty("CODPARC", parceiro);
			}
			  
			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
			
		} catch (Exception e) {
			salvarException("[gerarCabecalhoOS] não foi possível gerar o cabeçalho da OS. Patrimonio "+patrimonio+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
		}
		
		return numos;
	}
	
	private void geraItemOS(BigDecimal numos, BigDecimal osModelo, BigDecimal motivo, BigDecimal servico, BigDecimal executante, 
			BigDecimal codprod, String patrimonio){
		
		Timestamp dataAtual= TimeUtils.getNow();
		
		cadastraServicoParaOhExecutante(codprod,executante,servico);
			
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{osModelo,new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);
			NotaProdVO.setProperty("NUMITEM",new BigDecimal(1));
			NotaProdVO.setProperty("HRINICIAL", null); 
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", addDias(dataAtual,7));
			NotaProdVO.setProperty("INICEXEC", null); 
			NotaProdVO.setProperty("TERMEXEC", null); 
			NotaProdVO.setProperty("SERIE", patrimonio);
			NotaProdVO.setProperty("CODPROD", codprod);
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", motivo);
			NotaProdVO.setProperty("CODSERV", servico);
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", executante);
			NotaProdVO.setProperty("CODUSUREM", new BigDecimal(815));
			NotaProdVO.setProperty("CORSLA", new BigDecimal(11909048));
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			salvarException("[geraItemOS] não foi possível obter o executante. Núm. OS "+numos+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
		}
	}
	
	private void cadastraServicoParaOhExecutante(BigDecimal produto, BigDecimal atendente, BigDecimal servico) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ServicoProdutoExecutante");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODSERV", servico);
			VO.setProperty("CODUSU", atendente);
			VO.setProperty("CODPROD", produto);
			
			dwfFacade.createEntity("ServicoProdutoExecutante", (EntityVO) VO);
		} catch (Exception e) {
			//salvarException("[cadastraServicoParaOhExecutante] - Nao foi possivel cadastrar o serviço para o executante. "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private BigDecimal getCodprod(String patrimonio) throws Exception {
		BigDecimal produto = BigDecimal.ZERO;
		
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		if(VO!=null) {
			produto = VO.asBigDecimal("CODPROD");
		}
		
		return produto;
	}
	
	private Timestamp addDias(Timestamp datainicial,int prazo){
		Timestamp dataA = new Timestamp(TimeUtils.addWorkingDays(datainicial.getTime(), prazo));	
		return dataA;
	}	
	
	private BigDecimal getExecutanteChamadoTecnico(String patrimonio) {
		BigDecimal executante = null;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT DECODE(P.AD_GRPOPER,"+
					"42,56,"+
					"41,57,"+
					"40,58,"+
					"43,60,"+
					"46,186,"+
					"39,299,"+
					"45,471,"+
					"44,615,"+
					"50,772,"+
					"58,951,"+
					"54,956,"+
					"56) AS CODUSU "+
					"FROM AD_ENDERECAMENTO T "+
					"JOIN AD_PLANTAS P ON (P.NUMCONTRATO=T.NUMCONTRATO AND P.ID=T.ID) "+
					"WHERE T.CODBEM='"+patrimonio+"'"
					);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				executante = contagem.getBigDecimal("CODUSU");
			}
			
		} catch (Exception e) {
			salvarException("[getExecutanteChamadoTecnico] não foi possível obter o executante. Patrimonio "+patrimonio+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
		}
		
		if(executante==null) {
			executante = new BigDecimal(56);
		}
		
		return executante;
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
			salvarException("[getEnderecoDaMaquina] não foi possível obter o endereço. Patrimonio "+patrimonio+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
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
			salvarException("[getContrato] não foi possível obter o contrato. Patrimonio "+patrimonio+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
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
			salvarException("[getParceiro] não foi possível obter o parceiro. Patrimonio "+patrimonio+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
		}
		
		return parc;
	}
	
	private BigDecimal getCodParc(String patrimonio) {
		BigDecimal parc = BigDecimal.ZERO;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT P.CODPARC AS PARCEIRO FROM AD_ENDERECAMENTO T LEFT JOIN TCSCON C ON (C.NUMCONTRATO=T.NUMCONTRATO) LEFT JOIN TGFPAR P ON (P.CODPARC=C.CODPARC) WHERE T.CODBEM='"+patrimonio+"' AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				parc = contagem.getBigDecimal("PARCEIRO");
			}

		} catch (Exception e) {
			salvarException("[getCodParc] não foi possível obter cód. parceiro. Patrimonio "+patrimonio+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
		}
		
		return parc;
	}
	
	private BigDecimal getMenorContato(BigDecimal codparc) {
		BigDecimal parc = null;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT MIN(CODCONTATO) AS CODCONTATO FROM TGFCTT WHERE CODPARC="+codparc);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				parc = contagem.getBigDecimal("CODCONTATO");
			}

		} catch (Exception e) {
			salvarException("[getMenorContato] não foi possível obter o contato. "+codparc+
					"\n"+e.getMessage()+
					"\n"+e.getCause());
		}
		
		if(parc==null) {
			parc = new BigDecimal(1);
		}
		
		return parc;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_criaOS");
			VO.setProperty("PACOTE", "br.com.gsn.app.gcDigital");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
