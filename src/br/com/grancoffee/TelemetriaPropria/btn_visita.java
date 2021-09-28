package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Timer;

import com.sankhya.util.TimeUtils;
import Helpers.WSPentaho;
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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_visita implements AcaoRotinaJava {
	/**
	 * 23/09/21 vs 1.6 Inserido o método validaSeAhMaquinaEstaNaRota que valida se a máquina esta em uma rota, se não estiver ele impede a geração da visita.
	 */
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
			boolean maquinaNaRota = validaSeAhMaquinaEstaNaRota(linhas[i].getCampo("CODBEM").toString());

			if (visitaPendente > 0) {
				arg0.mostraErro("O Patrimônio <b>"+linhas[i].getCampo("CODBEM").toString()+"</b> já possui uma visita pendente! não é possível gerar outra!");
			} else {
				if(!maquinaNaRota) {
					arg0.mostraErro("Patrimônio <b>"+linhas[i].getCampo("CODBEM").toString()+"</b> fora da Rota, não pode ser gerado uma visita!");
				}else {
					BigDecimal idretorno = cadastrarNovaVisita(linhas[i].getCampo("CODBEM").toString());
					if(idretorno!=null) {
						carregaTeclasNosItensDeAbast(linhas[i].getCampo("CODBEM").toString(),idretorno);
						agendarVisita(linhas[i].getCampo("CODBEM").toString(), dtVisita, motivo,idretorno);
						cont++;
						
						Timer timer = new Timer(5000, new ActionListener() {	
							@Override
							public void actionPerformed(ActionEvent e) {
								chamaPentaho();				
							}
						});
						timer.setRepeats(false);
						timer.start();
						
					}
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
			salvarException("[validaSeAhMaquinaEstaNaRota] Não foi possivel verificar se a maquina "+patrimonio+" esta na rota. "+e.getMessage()+"\n"+e.getCause());
		}
		return valida;	
	}

	private void agendarVisita(String patrimonio, Timestamp dtVisita, String motivo,BigDecimal idretorno) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitacoesAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("DTSOLICIT", TimeUtils.getNow());
			VO.setProperty("DTAGENDAMENTO", dtVisita);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("STATUS", "1");
			VO.setProperty("ROTA", new BigDecimal(getRota(patrimonio)));
			VO.setProperty("MOTIVO", motivo.toCharArray());
			VO.setProperty("IDABASTECIMENTO", idretorno);
			VO.setProperty("APENASVISITA", "S");
			VO.setProperty("REABASTECIMENTO", "N");

			dwfFacade.createEntity("GCSolicitacoesAbastecimento", (EntityVO) VO);

		} catch (Exception e) {
			salvarException("[agendarVisita] Não foi possivel agendar a visita! "+e.getMessage()+"\n"+e.getCause());
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
					+ patrimonio + "') AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				count = contagem.getInt("ID");
			}

		} catch (Exception e) {
			salvarException("[getRota] Não foi possivel obter a rota! "+e.getMessage()+"\n"+e.getCause());
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
			VO.setProperty("NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("CODPARC", getParceiro(patrimonio));

			if (rota != 0) {
				VO.setProperty("ROTA", new BigDecimal(rota));
			}

			dwfFacade.createEntity("AD_RETABAST", (EntityVO) VO);

			idAbastecimento = VO.asBigDecimal("ID");

		} catch (Exception e) {
			salvarException("[cadastrarNovaVisita] Não foi possivel registrar o retorno! "+e.getMessage()+"\n"+e.getCause());
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
				salvarException("[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos! "+e.getMessage()+"\n"+e.getCause());
			}

		}
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

			String path = "home/GC_New/Transformation/Sankhya-Apenas_Visita/";
			String objName = "J-Loop_Apenas_visita";

			si.runJob(path, objName);

		} catch (Exception e) {
			salvarException("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_visita");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [salvarException] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}

}
