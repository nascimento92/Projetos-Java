package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_visita implements AcaoRotinaJava {
	int cont = 0;

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		start(arg0);
	}

	private void start(ContextoAcao arg0) throws Exception {
		Timestamp dtVisita = (Timestamp) arg0.getParam("DTVISITA");
		String motivo = (String) arg0.getParam("MOTIVO");

		Registro[] linhas = arg0.getLinhas();

		for (int i = 0; i < linhas.length; i++) {

			int visitaPendente = validaSeExisteVisitasPendentes(linhas[i].getCampo("CODBEM").toString());

			if (visitaPendente > 0) {
				boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!",
						"Patrimonio <b>" + linhas[i].getCampo("CODBEM")
								+ "</b> possui visita pendente, mesmo assim agendar uma nova visita?",
						1);
				if (confirmarSimNao) {
					BigDecimal idretorno = cadastrarNovaVisita(linhas[i].getCampo("CODBEM").toString());
					if(idretorno!=null) {
						carregaTeclasNosItensDeAbast(linhas[i].getCampo("CODBEM").toString(),idretorno);
						agendarVisita(linhas[i].getCampo("CODBEM").toString(), dtVisita, motivo,idretorno);
						cont++;
					}
					
				}
			} else {
				BigDecimal idretorno = cadastrarNovaVisita(linhas[i].getCampo("CODBEM").toString());
				if(idretorno!=null) {
					carregaTeclasNosItensDeAbast(linhas[i].getCampo("CODBEM").toString(),idretorno);
					agendarVisita(linhas[i].getCampo("CODBEM").toString(), dtVisita, motivo,idretorno);
					cont++;
				}
			}
		}
		
		if(cont>0) {
			arg0.setMensagemRetorno("Foram agendado(s) <b>" + cont + "</b> visita(s)!");
		}else {
			arg0.setMensagemRetorno("Não foram agendadas visitas!");
		}
	
	}

	private int validaSeExisteVisitasPendentes(String patrimonio) throws Exception {
		int quantidade = 0;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio + "' AND STATUS='1' AND APENASVISITA='S'");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			quantidade = contagem.getInt("QTD");
		}

		return quantidade;
	}

	private void agendarVisita(String patrimonio, Timestamp dtVisita, String motivo,BigDecimal idretorno) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitacoesAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("DTSOLICIT", dtVisita);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("STATUS", "1");
			VO.setProperty("ROTA", new BigDecimal(getRota(patrimonio)));
			VO.setProperty("MOTIVO", motivo.toCharArray());
			VO.setProperty("IDABASTECIMENTO", idretorno);
			VO.setProperty("APENASVISITA", "S");
			VO.setProperty("REABASTECIMENTO", "N");

			dwfFacade.createEntity("GCSolicitacoesAbastecimento", (EntityVO) VO);

		} catch (Exception e) {
			System.out.println("## [btn_visita] ## - Não foi possivel agendar a visita!");
			e.getMessage();
			e.getCause();
		}
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
					+ patrimonio + "') AND auditoria='S'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				count = contagem.getInt("ID");
			}

		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
		}

		return count;
	}

	private BigDecimal cadastrarNovaVisita(String patrimonio) {
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
			VO.setProperty("APENASVISITA", "S");

			if (rota != 0) {
				VO.setProperty("ROTA", new BigDecimal(rota));
			}

			dwfFacade.createEntity("AD_RETABAST", (EntityVO) VO);

			idAbastecimento = VO.asBigDecimal("ID");

		} catch (Exception e) {
			System.out.println("## [btn_visita] ## - Não foi possivel registrar o retorno!");
			e.getMessage();
			e.printStackTrace();
		}

		return idAbastecimento;
	}

	private void carregaTeclasNosItensDeAbast(String patrimonio, BigDecimal idAbastecimento) throws Exception {

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
				new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = DynamicVO.asString("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			//BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
			//BigDecimal nivelPar = DynamicVO.asBigDecimal("NIVELPAR");
			
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
				System.out.println(
						"## [btn_visita] ## - Nao foi possivel salvar as teclas na tela Retornos!");
				e.getMessage();
				e.printStackTrace();
			}

		}
	}

}
