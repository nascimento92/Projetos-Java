package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;

public class acaoAgendada_agendaVisita implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext arg0) {
		
		ServiceContext sctx = new ServiceContext(null); 		
		sctx.setAutentication(AuthenticationInfo.getCurrent()); 
		sctx.makeCurrent();
		
		try {
			SPBeanUtils.setupContext(sctx);
		} catch (Exception e) {
			e.printStackTrace();
			salvarException("[onTime] não foi possível setar o usuário! "+e.getMessage()+"\n"+e.getCause());
		} 
		
		JapeSession.SessionHandle hnd = null;
		
		try {

			hnd = JapeSession.open();
			hnd.execWithTX(new JapeSession.TXBlock() {

				public void doWithTx() throws Exception {

					getListaPendente();
				}

			});
			

		} catch (Exception e) {
			salvarException("[onTime] não foi possível iniciar a sessão! "+e.getMessage()+"\n"+e.getCause());
		}
		
	}
	
	private void getListaPendente() {
		String patrimonio;
		String valor;
		String abastecimento;
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT CODBEM, VALOR, ABASTECIMENTO FROM GC_GERA_ROTA");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				
				patrimonio = contagem.getString("CODBEM");
				valor = contagem.getString("VALOR");
				abastecimento = contagem.getString("ABASTECIMENTO");
				
				registraSolicitacoes(patrimonio,valor,abastecimento);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private void registraSolicitacoes(String patrimonio, String valor, String abastecimento) {
		String Hora = valor.substring(1)+":00:00";
		int QtdDias = Integer.valueOf(valor.substring(0, 1));
		Timestamp dataAgendamento = buildData(Hora);
		Timestamp dataAtendimento = TimeUtils.dataAddDay(dataAgendamento, QtdDias);
		
		if("1".equals(abastecimento)) { //Apenas Secos
			
		}
		
		if ("2".equals(abastecimento)) { // Apenas Congelados

		}
		
		if ("3".equals(abastecimento)) { // Secos e Congelados

		}
		
		
	}
	
	private static Timestamp buildData(String hora) {
		String formato1 = "yyyy-MM-dd";
		DateFormat df = new SimpleDateFormat(formato1);
		String dtAtual = df.format(TimeUtils.getNow());
		
		String dataEHora = dtAtual+" "+hora;
		
		Timestamp time = Timestamp.valueOf(dataEHora);
		
		return time;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "acaoAgendada_agendaVisita");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
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
