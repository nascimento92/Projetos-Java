package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_abastecimento implements AcaoRotinaJava{
	
	/**
	 * 23/09/21 vs 3.0 Inserido o m�todo validaSeAhMaquinaEstaNaRota que valida se a m�quina esta em uma rota, se n�o estiver ele impede a gera��o da visita.
	 * 30/09/21 vs 3.1 Inserido no m�todo agendarAbastecimento o preenchimento dos campos relacionados ao c�digo do parceiro e c�digo do contrato.
	 * 30/09/21 vs 3.2 Muda o objeto que o pentaho chama.
	 */
	
	String retornoNegativo="";
	int cont=0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		start(linhas,arg0);
		
		if(cont>0) {
			arg0.setMensagemRetorno("Foram solicitado(s) <b>"+cont+"</b> abastecimento(s)!");
		}
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		
		String tipoAbastecimento = (String) arg0.getParam("TIPABAST");//1=Agora	2=Agendado
		String secosCongelados = (String) arg0.getParam("SECOSECONGELADOS");//1=Abastecer Apenas Secos.2=Abastecer Apenas Congelados.3=Abastecer Secos e Congelados.

		for(int i=0; i<linhas.length; i++) {
			
			boolean maquinaNaRota = validaSeAhMaquinaEstaNaRota(linhas[i].getCampo("CODBEM").toString());
			
			if(!maquinaNaRota) {
				throw new Error("O patrim�nio "+linhas[i].getCampo("CODBEM").toString()+" n�o est� em rota, n�o pode ser gerado o abastecimento!");
			}
			
			BigDecimal idflow = (BigDecimal) linhas[i].getCampo("AD_IDFLOW");
			Timestamp dtAbastecimento = validacoes(linhas[i],arg0, tipoAbastecimento, secosCongelados);
			
			if("1".equals(secosCongelados)) { //apenas secos
				BigDecimal idAbastecimento = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "S", "N", idflow);//salva tela Abastecimento
				
				if(idAbastecimento!=null) {
					apenasSecos(idAbastecimento,dtAbastecimento,linhas[i].getCampo("CODBEM").toString());//carrega itens
					
					if(dtAbastecimento!=null) {//agendado
						agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),dtAbastecimento,idAbastecimento, "S", "N");
					}else {//agora
						agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),TimeUtils.getNow(),idAbastecimento, "S", "N");
					}
					cont++;
				}
			}
			
			else if ("2".equals(secosCongelados)) {//apenas congelados
				BigDecimal idAbastecimento = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "N", "S", idflow);

				if(idAbastecimento!=null) {
					apenasCongelados(idAbastecimento,dtAbastecimento,linhas[i].getCampo("CODBEM").toString());//carrega itens
					
					if(dtAbastecimento!=null) {//agendado
						agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),dtAbastecimento,idAbastecimento, "N", "S");
					}else {//agora
						agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),TimeUtils.getNow(),idAbastecimento, "N", "S");
					}
					cont++;
				}
			}
			
			else { //secos e congelados
				BigDecimal idsecos = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "S", "N", idflow);
				if (idsecos != null) {
					apenasSecos(idsecos,dtAbastecimento,linhas[i].getCampo("CODBEM").toString());
						if(dtAbastecimento!=null) {//agendado
							agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),dtAbastecimento,idsecos, "S", "N");
						}else {//agora
							agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),TimeUtils.getNow(),idsecos, "S", "N");
						}
						cont++;
				}
				
				
				BigDecimal idcongelados = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "N", "S", idflow);
				if (idcongelados != null) {
					apenasCongelados(idcongelados,dtAbastecimento,linhas[i].getCampo("CODBEM").toString());
						if(dtAbastecimento!=null) {//agendado
							agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),dtAbastecimento,idcongelados, "N", "S");
						}else {//agora
							agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(),TimeUtils.getNow(),TimeUtils.getNow(),idcongelados, "N", "S");
						}
						cont++;
				}	
			}
			
			linhas[i].setCampo("AD_IDFLOW", null);
		}
		chamaPentaho();
	}
	
	private boolean validaSeAhMaquinaEstaNaRota(String patrimonio) throws Exception {
		boolean valida = false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT CASE WHEN EXISTS(SELECT CODBEM FROM AD_ROTATELINS WHERE CODBEM='"+patrimonio+"') THEN 'S' ELSE 'N' END AS VALIDA FROM DUAL");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String verifica = contagem.getString("VALIDA");
				if("S".equals(verifica)) {
					valida=true;
				}
			}
			
		} catch (Exception e) {
			salvarException("[validaSeAhMaquinaEstaNaRota] N�o foi possivel verificar se a maquina "+patrimonio+" esta na rota. "+e.getMessage()+"\n"+e.getCause());
		}
		return valida;	
	}
	
	private Timestamp validacoes(Registro linhas,ContextoAcao arg0, String tipoAbastecimento, String secosCongelados) throws PersistenceException {
		
		Timestamp dtAbastecimento = (Timestamp) arg0.getParam("DTABAST");
		Timestamp dtSolicitacao=null;

			if(validaPedido(linhas.getCampo("CODBEM").toString(), secosCongelados)) {
				throw new PersistenceException("<br/>Patrim�nio <b>"+linhas.getCampo("CODBEM")+"</b> j� possui pedido pendente!<br/>");
			}	
			
			if("1".equals(tipoAbastecimento) && dtAbastecimento!=null) {
				dtAbastecimento=null;
			}
			
			if("2".equals(tipoAbastecimento) && dtAbastecimento==null) {
				throw new PersistenceException("<b>ERRO!</b> - Pedidos agendados precisam de uma data de abastecimento!");
			}
			
			if("2".equals(tipoAbastecimento) && dtAbastecimento!=null) {
				dtSolicitacao = TimeUtils.getNow();
				
				if(dtAbastecimento.before(reduzUmDia(dtSolicitacao))) {
					throw new PersistenceException("<b>ERRO!</b> - Data de agendamento n�o pode ser menor que a data de hoje!");
				}
				
				int diaDataAgendada = TimeUtils.getDay(dtAbastecimento);
				int diaDataAtual = TimeUtils.getDay(dtSolicitacao);
				
				int minutoDataAgendada = TimeUtils.getTimeInMinutes(dtAbastecimento);
				int minutoDataAtual = TimeUtils.getTimeInMinutes(dtSolicitacao);
				
				if(diaDataAtual==diaDataAgendada) {
					if(minutoDataAtual==minutoDataAgendada) {
						dtAbastecimento = null;
				}
			}
		}
			
			return dtAbastecimento;
	}
	
	private BigDecimal cadastrarNovoAbastecimento(String patrimonio, String secos, String congelados, BigDecimal idflow) {
		BigDecimal idAbastecimento = null;
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_RETABAST");
			DynamicVO VO = (DynamicVO) NPVO;
			
			int rota = getRota(patrimonio);
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("DTSOLICITACAO", TimeUtils.getNow());
			VO.setProperty("STATUS", "1");
			VO.setProperty("SOLICITANTE", getUsuLogado());
			VO.setProperty("NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("CODPARC", getParceiro(patrimonio));
			
			if(rota!=0) {
				VO.setProperty("ROTA", new BigDecimal(rota));
			}
			
			if(idflow!=null) {
				VO.setProperty("IDFLOW", idflow);
			}
			
			VO.setProperty("SECOS", secos);
			VO.setProperty("CONGELADOS", congelados);
						
			dwfFacade.createEntity("AD_RETABAST", (EntityVO) VO);
			
			idAbastecimento = VO.asBigDecimal("ID");
			
		} catch (Exception e) {
			retornoNegativo = retornoNegativo+e.getMessage();
			salvarException("[cadastrarNovoAbastecimento] Nao foi possivel cadastrar um novo abastecimento! " + e.getMessage() + "\n" + e.getCause());
		}
		
		return idAbastecimento;
	}
	
	private void apenasSecos(BigDecimal idAbastecimento, Timestamp dtAbastecimento, String patrimonio)
			throws Exception {
		if (idAbastecimento != null) {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				if (!congelado(produto)) {
					try {

						EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
						EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSRETABAST");
						DynamicVO VO = (DynamicVO) NPVO;

						VO.setProperty("ID", idAbastecimento);
						VO.setProperty("CODBEM", patrimonio);
						VO.setProperty("TECLA", tecla);
						VO.setProperty("CODPROD", produto);

						dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);

					} catch (Exception e) {
						salvarException(
								"[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos Abastecimento! "
										+ e.getMessage() + "\n" + e.getCause());
					}
				}
			}
		}
	}
	
	private void apenasCongelados(BigDecimal idAbastecimento, Timestamp dtAbastecimento, String patrimonio)
			throws Exception {
		if (idAbastecimento != null) {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				if (congelado(produto)) {
					try {

						EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
						EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSRETABAST");
						DynamicVO VO = (DynamicVO) NPVO;

						VO.setProperty("ID", idAbastecimento);
						VO.setProperty("CODBEM", patrimonio);
						VO.setProperty("TECLA", tecla);
						VO.setProperty("CODPROD", produto);

						dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);

					} catch (Exception e) {
						salvarException(
								"[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos Abastecimento! "
										+ e.getMessage() + "\n" + e.getCause());
					}
				}
			}
		}
	}
	
	private boolean congelado(BigDecimal codprod) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { codprod });
		BigDecimal grupo = VO.asBigDecimal("CODGRUPOPROD");
		
		DAO = JapeFactory.dao("GrupoProduto");
		DynamicVO VOS = DAO.findOne("CODGRUPOPROD=?",new Object[] { grupo });
		String congelado = VOS.asString("AD_CONGELADOS");
		
		if("S".equals(congelado)) {
			valida=true;
		}
		return valida;
	}

	private boolean validaPedido(String patrimonio, String secosCongelados) {
		boolean valida=false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			
			if("1".equals(secosCongelados)) {
				nativeSql.appendSql(
						"SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='"+patrimonio+"' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('1') AND NVL(AD_TIPOPRODUTOS,'1')='1'");
			}else if("2".equals(secosCongelados)) {
				nativeSql.appendSql(
						"SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='"+patrimonio+"' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('2') AND NVL(AD_TIPOPRODUTOS,'1')='1'");
			}else {
				nativeSql.appendSql(
						"SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='"+patrimonio+"' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('1','2') AND NVL(AD_TIPOPRODUTOS,'1')='1'");
			}
			
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			salvarException("[validaPedido] Nao foi possivel validar o pedido! " + e.getMessage() + "\n" + e.getCause());
		}
		
		return valida;
	}
	
	private BigDecimal agendarAbastecimento(String patrimonio, Timestamp dtSolicitacao, Timestamp dtAgendamento,BigDecimal idAbastecimento, String seco, String congelado) {
		BigDecimal idSolicitAbast = BigDecimal.ZERO;
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitacoesAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("STATUS", "1");
			VO.setProperty("DTSOLICIT", dtSolicitacao);
			VO.setProperty("DTAGENDAMENTO", dtAgendamento);
			VO.setProperty("ROTA", new BigDecimal(getRota(patrimonio)));
			VO.setProperty("IDABASTECIMENTO", idAbastecimento);
			VO.setProperty("REABASTECIMENTO", "S");
			VO.setProperty("APENASVISITA", "N");
			VO.setProperty("AD_NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("AD_CODPARC", getParceiro(patrimonio));
			
			if("S".equals(seco)) {
				VO.setProperty("AD_TIPOPRODUTOS", "1");
			}
			
			if("S".equals(congelado)) {
				VO.setProperty("AD_TIPOPRODUTOS", "2");
			}
			
			String body = montarBody(patrimonio);
			if(body!=null) {
				VO.setProperty("AD_BODYPLANOGRAMA", body.toCharArray());
			}
			
			dwfFacade.createEntity("GCSolicitacoesAbastecimento", (EntityVO) VO);
			
			idSolicitAbast = VO.asBigDecimal("ID");
					
		} catch (Exception e) {
			salvarException("[agendarAbastecimento] Nao foi possivel agendar o Abastecimento! patrimonio: "+patrimonio+"\n"+ e.getMessage() + "\n" + e.getCause());
		}
		
		return idSolicitAbast;
	}
		
	private Timestamp reduzUmDia(Timestamp data) {
		Calendar dataAtual = Calendar.getInstance();
		dataAtual.setTime(data);
		dataAtual.add(Calendar.DAY_OF_MONTH, -1);
		return new Timestamp(dataAtual.getTimeInMillis());
	}
				
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}

	private int getRota(String patrimonio) {
		int count=0;
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT ID FROM AD_ROTATEL WHERE ID IN (SELECT ID FROM AD_ROTATELINS WHERE codbem='"+patrimonio+"') AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				count = contagem.getInt("ID");
			}

		} catch (Exception e) {
			salvarException("[getRota] Nao foi possibel obter a Rota! "+e.getMessage()+"\n"+e.getCause());
		}
		
		return count;
	}
	
	private BigDecimal getContrato(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		return contrato;
	}
	
	private BigDecimal getParceiro(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		
		DAO = JapeFactory.dao("Contrato");
		DynamicVO VOS = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		BigDecimal parceiro = VOS.asBigDecimal("CODPARC");
		
		return parceiro;
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");;
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-Pedido/";
			String objName = "J-Loop_visitas_pendentes";

			si.runJob(path, objName);

		} catch (Exception e) {
			salvarException("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public String montarBody(Object codbem){
		
		int cont = 1;
		String head="{\"planogram\":{\"items_attributes\": [";
		String bottom="]}}";
		
		String body = "";
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("teclas", "this.CODBEM = ? ", new Object[] { codbem }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				String teclaAlternativa = DynamicVO.asString("TECLAALT");
				String tecla = DynamicVO.asBigDecimal("TECLA").toString();
				String name = "";
				
				if("0".equals(tecla)) {
					tecla=String.valueOf(cont);
					cont++;
				}
				
				if(teclaAlternativa!=null) {
					name = teclaAlternativa;
				}else {
					name = tecla;
				}
				
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				
				
				if(Iterator.hasNext()) {
					body=body+"{"+
							"\"type\": \"Coil\","+
							"\"name\": \""+name+"\","+
							"\"good_id\": "+getGoodId(produto)+","+
							"\"capacity\": "+DynamicVO.asBigDecimal("AD_CAPACIDADE").toString()+","+
							"\"par_level\": "+DynamicVO.asBigDecimal("AD_NIVELPAR").toString()+","+
							"\"alert_level\": "+DynamicVO.asBigDecimal("AD_NIVELALERTA").toString()+","+
							"\"desired_price\": "+DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString()+","+
							"\"logical_locator\": "+tecla+","+
							"\"status\": \"active\""
							+ "},";
				}else {
					body=body+"{"+
							"\"type\": \"Coil\","+
							"\"name\": \""+DynamicVO.asBigDecimal("TECLA").toString()+"\","+
							"\"good_id\": "+getGoodId(produto)+","+
							"\"capacity\": "+DynamicVO.asBigDecimal("AD_CAPACIDADE").toString()+","+
							"\"par_level\": "+DynamicVO.asBigDecimal("AD_NIVELPAR").toString()+","+
							"\"alert_level\": "+DynamicVO.asBigDecimal("AD_NIVELALERTA").toString()+","+
							"\"desired_price\": "+DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString()+","+
							"\"logical_locator\": "+tecla+","+
							"\"status\": \"active\""
							+ "}";
				}
			
			}
			
		} catch (Exception e) {
			salvarException("[montarBody] nao foi possivel montar o Body! patrimonio:"+codbem+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return head+body+bottom;

	}
	
	public BigDecimal getGoodId(BigDecimal produto) throws Exception {
		BigDecimal id = null;
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		BigDecimal idVerti = VO.asBigDecimal("AD_IDPROVERTI");
		
		if(idVerti==null) {
			id = new BigDecimal(179707);
		}else {
			id = idVerti;
		}
		
		return id;
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_abastecimento");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui n�o tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}
}
