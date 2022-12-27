package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

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
import br.com.sankhya.ws.ServiceContext;

public class btn_abastecimento_teste implements AcaoRotinaJava{
	
	String retornoNegativo = "";
	int cont = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		start(linhas, arg0);

		if (cont > 0) {
			arg0.setMensagemRetorno("Foram solicitado(s) <b>" + cont + "</b> abastecimento(s)!");
		}
		
	}
	
	private void start(Registro[] linhas, ContextoAcao arg0) throws Exception {
		String tipoAbastecimento = (String) arg0.getParam("TIPABAST");// 1=Agora 2=Agendado
		String secosCongelados = (String) arg0.getParam("SECOSECONGELADOS");// 1=Abastecer Apenas Secos.2=Abastecer Apenas Congelados.3=Abastecer Secos e Congelados. 4 = Tabaco
		Timestamp dtAbastecimentoX = (Timestamp) arg0.getParam("DTABAST");
		
		DynamicVO gc_solicitabast = null;
		BigDecimal idRetorno = null;
		
		//TODO :: Implementar a data da visita
		Timestamp dtvisita = (Timestamp) arg0.getParam("DTVISIT");
		
		if (dtvisita != null) {
			if (dtvisita.before(TimeUtils.getNow())) {
				dtvisita = addDias(TimeUtils.getNow(), new BigDecimal(1));
			}

			if (dtAbastecimentoX != null) {
				
				Timestamp dataSemHoras = ajustaDataRetirandoHoras(dtAbastecimentoX);
				
				if (dtvisita.before(dataSemHoras)) {
					dtvisita = addDias(dtAbastecimentoX, new BigDecimal(1));
				}
			}
			
		} else {
			Timestamp datatemp = null;

			if ("1".equals(tipoAbastecimento)) {
				datatemp = TimeUtils.getNow();
			} else {
				datatemp = dtAbastecimentoX;
			}

			dtvisita = addDias(datatemp, new BigDecimal(1));
		}
		
		for (int i = 0; i < linhas.length; i++) {
			boolean maquinaDesabastecida = verificarSeAhMaquinaEstaTotalmenteDesabastecida(linhas[i].getCampo("CODBEM").toString());
			
			BigDecimal idflow = (BigDecimal) linhas[i].getCampo("AD_IDFLOW");	
			Timestamp dtAbastecimento = validacoes(linhas[i], arg0, tipoAbastecimento, secosCongelados);
			
			if ("1".equals(secosCongelados)) { 
				//retorno abastecimento
				idRetorno = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "S", "N", idflow, "N"); 
				
				if (idRetorno != null) {
					//itens retorno abastecimento
					apenasSecos(idRetorno, dtAbastecimento, linhas[i].getCampo("CODBEM").toString());
				}
				
				/*
				throw new Error("[param] TIPABAST: "+tipoAbastecimento+
						"<br/>[param] SECOSECONGELADOS: "+secosCongelados+
						"<br/>[param] DTABAST: "+dtAbastecimentoX+
						"<br/>[param] DTVISIT: "+dtvisita+
						"<br/>[result] CODBEM: "+linhas[i].getCampo("CODBEM").toString()+
						"<br/>[result] MAQ DESABASTECIDA: "+maquinaDesabastecida+
						"<br/>[result] ID FLOW: "+idflow+
						"<br/>[result] DT ABASTECIMENTO: "+dtAbastecimento+
						"<br/>[result] ID RETORNO: "+idRetorno);
				*/
			}	
		}
	}
	
	private void apenasSecos(BigDecimal idAbastecimento, Timestamp dtAbastecimento, String patrimonio) throws Exception {
		if (idAbastecimento != null) {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("CODPROD"));
				BigDecimal capacidade = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("CAPACIDADE"));
				BigDecimal nivelpar = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("NIVELPAR"));
				BigDecimal vlrpar = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("VLRPAR"));
				BigDecimal vlrfun = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("VLRFUN"));
				BigDecimal valorFinal = BigDecimalUtil.getValueOrZero(vlrpar.add(vlrfun));
				
					try {

						EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
						EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSRETABAST");
						DynamicVO VO = (DynamicVO) NPVO;

						VO.setProperty("ID", idAbastecimento);
						VO.setProperty("CODBEM", patrimonio);
						VO.setProperty("TECLA", tecla);
						VO.setProperty("CODPROD", produto);
						VO.setProperty("CAPACIDADE", capacidade);
						VO.setProperty("NIVELPAR", nivelpar);
						VO.setProperty("VALOR", valorFinal);

						dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);

					} catch (Exception e) {
						/*
						 * salvarException(
						 * "[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos Abastecimento! "
						 * + e.getMessage() + "\n" + e.getCause());
						 */
					}
			}
		}
	}
	
	private BigDecimal cadastrarNovoAbastecimento(String patrimonio, String secos, String congelados,
			BigDecimal idflow, String tabaco) {
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

			if (rota != 0) {
				VO.setProperty("ROTA", new BigDecimal(rota));
			}

			if (idflow != null) {
				VO.setProperty("IDFLOW", idflow);
			}

			VO.setProperty("SECOS", secos);
			VO.setProperty("CONGELADOS", congelados);
			VO.setProperty("TABACO", tabaco);

			dwfFacade.createEntity("AD_RETABAST", (EntityVO) VO);

			idAbastecimento = VO.asBigDecimal("ID");

		} catch (Exception e) {
			retornoNegativo = retornoNegativo + e.getMessage();
			/*
			 * salvarException("[cadastrarNovoAbastecimento] Nao foi possivel cadastrar um novo abastecimento! "
			 * + e.getMessage() + "\n" + e.getCause());
			 */
		}

		return idAbastecimento;
	}
	
	private BigDecimal getParceiro(String patrimonio) throws Exception {
		BigDecimal parceiro = null;
		BigDecimal contrato = null;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
			DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });

			if(VO!=null) {
				contrato = VO.asBigDecimal("NUMCONTRATO");
			}
			
			if(contrato!=null) {
				DAO = JapeFactory.dao("Contrato");
				DynamicVO VOS = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
				parceiro = VOS.asBigDecimal("CODPARC");
			}
	
		} catch (Exception e) {
			/*
			 * salvarException("[getParceiro] nao foi possivel obter o parceiro:" +
			 * patrimonio + "\n" + e.getMessage() + "\n" + e.getCause());
			 */
		}
		
		if(parceiro==null) {
			parceiro = new BigDecimal(1);
		}

		return parceiro;
	}
	
	private BigDecimal getContrato(String patrimonio) throws Exception {
		BigDecimal contrato = null;
		try {
			JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
			DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
			contrato = VO.asBigDecimal("NUMCONTRATO");
		} catch (Exception e) {
			/*
			 * salvarException("[getContrato] nao foi possivel obter o contrato, patrimonio:"
			 * + patrimonio + "\n" + e.getMessage() + "\n" + e.getCause());
			 */
		}
		
		if(contrato==null) {
			contrato = new BigDecimal(1314);
		}
		
		return contrato;
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}
	
	private int getRota(String patrimonio) {
		int count = 0;
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT ID FROM AD_ROTATEL WHERE ID IN (SELECT ID FROM AD_ROTATELINS WHERE codbem='"
					+ patrimonio + "') AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				count = contagem.getInt("ID");
			}

		} catch (Exception e) {
			/*
			 * salvarException("[getRota] Nao foi possibel obter a Rota! " + e.getMessage()
			 * + "\n" + e.getCause());
			 */
		}

		return count;
	}
	
	private Timestamp validacoes(Registro linhas, ContextoAcao arg0, String tipoAbastecimento, String secosCongelados)
			throws Exception {
		
		//valida se a máquina está na rota.
		boolean maquinaNaRota = validaSeAhMaquinaEstaNaRota(linhas.getCampo("CODBEM").toString());
		if (!maquinaNaRota) {
			throw new Error("O patrimônio " + linhas.getCampo("CODBEM").toString()
					+ " não está em rota, não pode ser gerado o abastecimento!");
		}
		
		//valida se a máquina possuí planograma.
		boolean maquinaSemPlanograma = verificaSeAhMaquinaPossuiPlanograma(linhas.getCampo("CODBEM").toString());
		if(maquinaSemPlanograma) {
			throw new Error("<br/><b>ATENÇÃO</b><br/><br/>A máquina "+linhas.getCampo("CODBEM").toString()+" não possui um planograma cadastrado, não é possível gerar a visita!");
		}
		
		//se for loja, valida se não existem produtos repetidos.
		String loja = (String) linhas.getCampo("TOTEM");
		if("S".equals(loja)) {
			if(seExistemProdutosDuplicadoLojas(linhas.getCampo("CODBEM").toString())) {
				throw new Error("<br/><b>ATENÇÃO</b><br/><br/>A máquina "+linhas.getCampo("CODBEM").toString()+" está marcada como loja <b>Micro Market</b>, porém existem produtos duplicados no planograma, não é possível gerar a visita, ajustar o planograma! <br/><br/>");
			}
		}else {
			if(seExistemProdutosDuplicadoMaquina(linhas.getCampo("CODBEM").toString())) {
				throw new Error("<br/><b>ATENÇÃO</b><br/><br/>A máquina "+linhas.getCampo("CODBEM").toString()+" está com teclas duplicadas, não é possível gerar a visita, ajustar o planograma! <br/><br/>");
			}
		}
		
		//verifica se existe visita pendente sem ajste.
		if(validaSeExisteVisitaSemAjusteReabastecimento(linhas.getCampo("CODBEM").toString())) {
			throw new Error("<br/><b>ATENÇÃO</b><br/><br/>A máquina "+linhas.getCampo("CODBEM").toString()+" possuí uma visita de reabastecimento finalizada, porém pendente de ajuste por parte do setor de controladoria, não é possível gerar um novo abastecimento até que o setor de controladoria finalize o ajuste da visita! <br/><br/>");
		}
		
		String tp = "";
		
		if("1".equals(secosCongelados)) {
			tp="Secos";
		}else if ("4".equals(secosCongelados)) {
			tp="Tabaco";
		}else {
			tp="Congelados";
		}
		
		if("3".equals(secosCongelados)) {
			
			String valid="";
			
			for(int x=1; x<=2; x++) {
				
				if(x==1) {
					valid = "1";
				}else {
					valid = "2";
				}
				
				if(!validaSeOhPedidoDeAbastecimentoPoderaSerGerado(valid,linhas.getCampo("CODBEM").toString())) {
					throw new Error("<b> Atenção </b><br/><br/>"+
				" O pedido de <b>Secos</b> ou <b>Congelados</b> não pode ser gerado !<br/>"+
				" causas possíveis: <br/>"+
				"- A máquina esta totalmente abastecida.<br/>"+
				"- Os itens estão marcados para não abastecer.<br/>"+
				"- Os itens estão em ruptura.<br/>"+
				"- Os itens tem uma quantidade mínima para abastecimento, e essa quantidade não foi atingida. </br><br/>");
				}
			}
		}else {
			if(!validaSeOhPedidoDeAbastecimentoPoderaSerGerado(secosCongelados,linhas.getCampo("CODBEM").toString())) {
				throw new Error("<b> Atenção </b><br/><br/>"+
			" O pedido de <b>"+tp+"</b> não pode ser gerado !<br/>"+
			" causas possíveis: <br/>"+
			"- A máquina esta totalmente abastecida.<br/>"+
			"- Os itens estão marcados para não abastecer.<br/>"+
			"- Os itens estão em ruptura.<br/>"+
			"- Os itens tem uma quantidade mínima para abastecimento, e essa quantidade não foi atingida. </br><br/>");
			}
		}
		
		Timestamp dtAbastecimento = (Timestamp) arg0.getParam("DTABAST");
		Timestamp dtSolicitacao = null;
		

		if (validaPedido(linhas.getCampo("CODBEM").toString(), secosCongelados)) {
			throw new PersistenceException(
					"<br/>Patrimônio <b>" + linhas.getCampo("CODBEM") + "</b> já possui pedido pendente!<br/>");
		}

		if ("1".equals(tipoAbastecimento) && dtAbastecimento != null) {
			dtAbastecimento = null;
		}

		if ("2".equals(tipoAbastecimento) && dtAbastecimento == null) {
			throw new PersistenceException("<b>ERRO!</b> - Pedidos agendados precisam de uma data de abastecimento!");
		}

		if ("2".equals(tipoAbastecimento) && dtAbastecimento != null) {
			dtSolicitacao = TimeUtils.getNow();

			if (dtAbastecimento.before(reduzUmDia(dtSolicitacao))) {
				throw new PersistenceException(
						"<b>ERRO!</b> - Data de agendamento não pode ser menor que a data de hoje!");
			}

			int diaDataAgendada = TimeUtils.getDay(dtAbastecimento);
			int diaDataAtual = TimeUtils.getDay(dtSolicitacao);

			int minutoDataAgendada = TimeUtils.getTimeInMinutes(dtAbastecimento);
			int minutoDataAtual = TimeUtils.getTimeInMinutes(dtSolicitacao);

			if (diaDataAtual == diaDataAgendada) {
				if (minutoDataAtual == minutoDataAgendada) {
					dtAbastecimento = null;
				}
			}
		}

		return dtAbastecimento;
	}
	
	private Timestamp reduzUmDia(Timestamp data) {
		Calendar dataAtual = Calendar.getInstance();
		dataAtual.setTime(data);
		dataAtual.add(Calendar.DAY_OF_MONTH, -1);
		return new Timestamp(dataAtual.getTimeInMillis());
	}
	
	private boolean validaPedido(String patrimonio, String secosCongelados) {
		boolean valida = false;

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();

			if ("1".equals(secosCongelados)) {
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio
						+ "' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('1') AND NVL(AD_TIPOPRODUTOS,'1')='1'");
			} else if ("2".equals(secosCongelados)) {
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio
						+ "' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('2') AND NVL(AD_TIPOPRODUTOS,'1')='2'");
			} else if ("4".equals(secosCongelados)) {
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio
						+ "' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('3') AND NVL(AD_TIPOPRODUTOS,'1')='3'");
			}else {
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio
						+ "' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('1','2') AND NVL(AD_TIPOPRODUTOS,'1')='1'");
			}

			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			/*
			 * salvarException( "[validaPedido] Nao foi possivel validar o pedido! " +
			 * e.getMessage() + "\n" + e.getCause());
			 */
		}

		return valida;
	}
	
	private boolean validaSeOhPedidoDeAbastecimentoPoderaSerGerado(String secosCongelados, String codbem) {
		
		boolean valida = false;
		
		try {
			
			String congelado = "";
			String tabaco = "";
			
			if("1".equals(secosCongelados)) {
				congelado="N";
				tabaco = "N";
			}else if("4".equals(secosCongelados)) {
				congelado="N";
				tabaco = "S";
			}else {
				congelado="S";
				tabaco = "N";
			}
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
			"SELECT COUNT(*) AS QTD "+
			"FROM("+
				"SELECT X.CODBEM, X.CODPROD, X.FALTA, X.EMP_ABAST, X.LOCAL_ABAST, X.AD_CONGELADOS, X.AD_TABACO FROM("+
					"SELECT T.CODBEM, P.CODPROD,(P.NIVELPAR-P.ESTOQUE) AS FALTA, "+
					"CASE WHEN NVL(EO.CODEMPABAST,C.CODEMP)=1 AND '"+secosCongelados+"'='1' THEN 6 WHEN NVL(EO.CODEMPABAST,C.CODEMP) IN (1,6) AND '"+secosCongelados+"'='2' THEN 2 WHEN NVL(EO.CODEMPABAST,C.CODEMP)<>1 THEN NVL(EO.CODEMPABAST,C.CODEMP) END AS EMP_ABAST, "+
					"CASE WHEN GRU.AD_CONGELADOS='S' THEN NVL(EO.CODLOCALCONGELADOS,1110) WHEN GRU.AD_TABACO='S' THEN NVL(EO.CODLOCALTABACO,1110) ELSE NVL(EO.CODLOCALABAST,1110) END AS LOCAL_ABAST, NVL(GRU.AD_CONGELADOS,'N') AS AD_CONGELADOS, NVL(PRO.AD_QTDMIN,1) AS AD_QTDMIN, NVL(GRU.AD_TABACO,'N') AS AD_TABACO FROM GC_INSTALACAO T "+
					"JOIN GC_PLANOGRAMA P ON (P.CODBEM=T.CODBEM) JOIN AD_ENDERECAMENTO EO ON (EO.CODBEM=T.CODBEM) JOIN TCSCON C ON (C.NUMCONTRATO=EO.NUMCONTRATO) JOIN TGFPRO PRO ON (PRO.CODPROD=P.CODPROD) JOIN TGFGRU GRU ON (GRU.CODGRUPOPROD=PRO.CODGRUPOPROD) "+
					"WHERE T.CODBEM='"+codbem+"' AND P.AD_ABASTECER='S' AND (P.NIVELPAR - P.ESTOQUE)> 0 "+
				") X "+
				"LEFT JOIN TGFEST E ON (E.CODEMP=X.EMP_ABAST AND E.CODLOCAL=X.LOCAL_ABAST AND E.CODPROD=X.CODPROD AND E.CONTROLE=' ' AND E.ATIVO='S' AND E.CODPARC=0) "+
				"WHERE (E.ESTOQUE - E.RESERVADO) >= FALTA AND '"+congelado+"' = AD_CONGELADOS AND '"+tabaco+"' = AD_TABACO AND TRUNC(FALTA/AD_QTDMIN)>0 "+
			")");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			/*
			 * salvarException(
			 * "[validaSeOhPedidoDeAbastecimentoPoderaSerGerado] Nao foi possivel validar a quantidade de itens! Patrimonio "
			 * +codbem + e.getMessage() + "\n" + e.getCause());
			 */
		}
		
		return valida;
		
		//query:
		/*
		 * 
		 * SELECT 
COUNT(*) AS QTD
FROM(
	SELECT X.CODBEM, X.CODPROD, X.FALTA, X.EMP_ABAST, X.LOCAL_ABAST, X.AD_CONGELADOS, X.AD_TABACO
	FROM(
		SELECT 
		T.CODBEM, 
		P.CODPROD,
		(P.NIVELPAR-P.ESTOQUE) AS FALTA, 
		CASE 
			WHEN NVL(EO.CODEMPABAST,C.CODEMP)=1 AND '4'='1' THEN 6 
			WHEN NVL(EO.CODEMPABAST,C.CODEMP) IN (1,6) AND '4'='2' THEN 2 
			WHEN NVL(EO.CODEMPABAST,C.CODEMP)<>1 THEN NVL(EO.CODEMPABAST,C.CODEMP) 
			END AS EMP_ABAST,
		CASE 
			WHEN GRU.AD_CONGELADOS='S' THEN NVL(EO.CODLOCALCONGELADOS,1110)
			WHEN GRU.AD_TABACO='S' THEN NVL(EO.CODLOCALTABACO,1110)
			ELSE NVL(EO.CODLOCALABAST,1110)
		END AS LOCAL_ABAST,
		NVL(GRU.AD_CONGELADOS,'N') AS AD_CONGELADOS, 
		NVL(PRO.AD_QTDMIN,1) AS AD_QTDMIN,
		NVL(GRU.AD_TABACO,'N') AS AD_TABACO
		FROM GC_INSTALACAO T 
		JOIN GC_PLANOGRAMA P ON (P.CODBEM=T.CODBEM) 
		JOIN AD_ENDERECAMENTO EO ON (EO.CODBEM=T.CODBEM) 
		JOIN TCSCON C ON (C.NUMCONTRATO=EO.NUMCONTRATO) 
		JOIN TGFPRO PRO ON (PRO.CODPROD=P.CODPROD) 
		JOIN TGFGRU GRU ON (GRU.CODGRUPOPROD=PRO.CODGRUPOPROD) 
		WHERE 
		T.CODBEM='GCETESTE' AND 
		P.AD_ABASTECER='S' 
		AND (P.NIVELPAR - P.ESTOQUE)> 0 
		) X 
		LEFT JOIN TGFEST E ON (E.CODEMP=X.EMP_ABAST AND E.CODLOCAL=X.LOCAL_ABAST AND E.CODPROD=X.CODPROD AND E.CONTROLE=' ' AND E.ATIVO='S' AND E.CODPARC=0) 
		WHERE 
		(E.ESTOQUE - E.RESERVADO) >= FALTA AND 
		'S' = AD_CONGELADOS AND
		'N' = AD_TABACO AND
		TRUNC(FALTA/AD_QTDMIN)>0
)
		 */
		
	}
	
	private boolean validaSeExisteVisitaSemAjusteReabastecimento(String patrimonio) {
		boolean valida = false;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
			"SELECT COUNT(OK) AS QTD "+
			"FROM( SELECT CASE WHEN S.STATUS='3' AND R.STATUSVALIDACAO='2' THEN 'S' ELSE 'N' END AS OK"
			+ " FROM GC_SOLICITABAST S"
			+ " JOIN AD_RETABAST R ON (R.ID=S.IDABASTECIMENTO)"
			+ " WHERE S.CODBEM='"+patrimonio+"' AND R.NUMOS IS NOT NULL AND S.REABASTECIMENTO='S' AND S.STATUS='3') WHERE OK='N'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
			
		}
		return valida;
	}
	
	private boolean seExistemProdutosDuplicadoMaquina(String patrimonio) {
		boolean valida = false;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT TECLA, COUNT(*) AS QTD FROM GC_PLANOGRAMA WHERE CODBEM='"+patrimonio+"' GROUP BY TECLA HAVING COUNT(*)>1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count > 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
	
		}
		return valida;
	}
	
	private boolean seExistemProdutosDuplicadoLojas(String patrimonio) {
		boolean valida = false;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT CODPROD, COUNT(*) AS QTD FROM GC_PLANOGRAMA WHERE CODBEM='"+patrimonio+"' GROUP BY CODPROD HAVING COUNT(*)>1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count > 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
		
		}
		return valida;
	}
	
	private boolean verificaSeAhMaquinaPossuiPlanograma(String codbem) {
		boolean valida = false;
		
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM GC_PLANOGRAMA WHERE CODBEM='"+codbem+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count == 0) {
					valida = true;
				}
			}

		} catch (Exception e) {
			/*
			 * salvarException(
			 * "[verificaSeAhMaquinaPossuiPlanograma] Nao foi possivel validar a quantidade de itens no planograma! Patrimonio "
			 * + codbem + e.getMessage() + "\n" + e.getCause());
			 */
		}
		
		return valida;
		
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
			nativeSql.appendSql("SELECT CASE WHEN EXISTS(SELECT CODBEM FROM AD_ROTATELINS I JOIN AD_ROTATEL R ON (R.ID=I.ID) WHERE I.CODBEM='"+patrimonio+"' AND R.ROTATELPROPRIA='S') THEN 'S' ELSE 'N' END AS VALIDA FROM DUAL" );
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String verifica = contagem.getString("VALIDA");
				if ("S".equals(verifica)) {
					valida = true;
				}
			}

		} catch (Exception e) {
			/*
			 * salvarException("[validaSeAhMaquinaEstaNaRota] Não foi possivel verificar se a maquina "
			 * + patrimonio + " esta na rota. " + e.getMessage() + "\n" + e.getCause());
			 */
		}
		return valida;
	}
	
	private boolean verificarSeAhMaquinaEstaTotalmenteDesabastecida(String patrimonio) {
		boolean valida = false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
							"SELECT CASE WHEN QTD_VISITAS > 0 AND QTD_VAZIAS = QTD_PLANOGRAMA THEN 'S' ELSE 'N' END AS VALIDACAO "+ 
							"FROM(" + 
							"SELECT "+
							"I.CODBEM, "+
							"(SELECT COUNT(*) FROM GC_PLANOGRAMA WHERE CODBEM=I.CODBEM) AS QTD_PLANOGRAMA, "+
							"(SELECT COUNT(*) FROM GC_PLANOGRAMA WHERE CODBEM=I.CODBEM AND ESTOQUE=0) AS QTD_VAZIAS, "+
							"(SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM=I.CODBEM AND STATUS='3' AND REABASTECIMENTO='S' AND DTABAST > SYSDATE-15) AS QTD_VISITAS "+
							"FROM GC_INSTALACAO I) WHERE CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String validacao = contagem.getString("VALIDACAO");
				if ("S".equals(validacao)) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			/*
			 * salvarException("[verificarSeAhMaquinaEstaTotalmenteDesabastecida] Nao foi possivel verificar se a máquina esta totalmente desabastecida! patrimonio "
			 * + patrimonio + e.getMessage() + "\n" + e.getCause());
			 */
		}
		
		return valida;
	}
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	private Timestamp ajustaDataRetirandoHoras(Timestamp data) {
		int day = TimeUtils.getDay(data);
		int month = TimeUtils.getMonth(data);
		int year = TimeUtils.getYear(data);
		
		Timestamp buildData = TimeUtils.buildData(day, month-1, year);
		
		return buildData;
	}

}
