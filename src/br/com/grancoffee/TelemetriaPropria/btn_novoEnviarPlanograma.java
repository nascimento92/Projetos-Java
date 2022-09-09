package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;
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

public class btn_novoEnviarPlanograma implements AcaoRotinaJava {

	/**
	 * @autor Gabriel
	 * @motivo Botão para envio do planograma para a Verti / Uppay
	 * 
	 * 27/07/22 vs 1.1 - Gabriel Nascimento - Unificação do botão para enviar para a Verti e para a Uppay.
	 */
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String patrimonio = (String) linhas[0].getCampo("CODBEM");
		String body = "";
		
		boolean confirmarSimNao = arg0.confirmarSimNao("ATENÇÃO", "<br/><br/>O planograma será enviado para a Uppay (APP) e para a Verti (Totem) de forma <b>forçada</b>!"+
		"<br/><br/>Este planograma será efetivado imediatamente ! (No caso da Verti é obrigatório que não tenha <b>NENHUM</b> picklist ou planograma pendente, consultar se deu certo na tela \"Planogramas Verti - TP\""
		+ "<br/><br/>Se neste planograma atual ocorreu uma troca de grade, cuidado, pois se existir produtos físicos no cliente que estavam na grade anterior porém, nesta não, eles não passarão no totem na hora da bipagem."
		+ "<br/><br/>Esta ação não pode ser desfeita, deseja continuar ?<br/><br/>", 0);
		
		if(confirmarSimNao) {
			//TODO :: Envio para a Verti
			DynamicVO instalacaoVO = getGcInstalacao(patrimonio);
			String micromarketing = instalacaoVO.asString("TOTEM");
			
			if("S".equals(micromarketing)) {
				body = montarBody(patrimonio);
				cadastrarTeclas(patrimonio,body);
			}else {
				if(validaSeExistemTeclasDuplicadas(patrimonio)) {
					arg0.mostraErro("<br/><b>Existem teclas repetidas! não é possível continuar</b><br/>");
				}else {
					body = montarBody(patrimonio);
					cadastrarTeclas(patrimonio,body);
				}
			}
			
			//TODO :: Atualizar no Sankhya
			
			if("S".equals(micromarketing)) {
				marcarBotaoPendente(patrimonio);
			}
			
			arg0.setMensagemRetorno("Planograma Enviado!");
			
			//TODO :: Chamar pentaho
			
			Timer timer = new Timer(2000, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					chamaPentaho();
				}
			});
			timer.setRepeats(false);
			timer.start();

			//chamaPentaho();
		}
		
	}
	
	//Envio para a Verti
	
	private DynamicVO getGcInstalacao(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	public String montarBody(String codbem){
		
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
			throw new Error("[montarBody] nao foi possivel montar o Body! patrimonio:"+codbem+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return head+body+bottom;
	}

	public BigDecimal getGoodId(BigDecimal produto) throws Exception {
		BigDecimal id = null;
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { produto });
		BigDecimal idVerti = VO.asBigDecimal("AD_IDPROVERTI");

		if (idVerti == null) {
			id = new BigDecimal(179707);
		} else {
			id = idVerti;
		}

		return id;
	}
	
	private void cadastrarTeclas(String patrimonio, String body) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_PLANVERTI");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("BODY", body.toCharArray());
			VO.setProperty("INTEGRADO", "N");
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("DTSOLICIT", TimeUtils.getNow());
			
			dwfFacade.createEntity("AD_PLANVERTI", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[cadastrarTeclas] erro ao tentar cadastrar teclas: "+patrimonio+"\n"+e.getMessage()+" "+e.getCause());
		}
	}
	
	private boolean validaSeExistemTeclasDuplicadas(String patrimonio) {
		boolean valida = false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) AS QTD, TECLA FROM AD_TECLAS WHERE CODBEM='"+patrimonio+"' GROUP BY TECLA");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count > 1) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			salvarException("[validaSeExistemTeclasDuplicadas] erro ao verificar teclas duplicadas, patrimonio: "+patrimonio+"\n"+e.getMessage()+" "+e.getCause());
		}
		
		return valida;
	}
	
	//fim -- Envio para a Verti
	

	public void marcarBotaoPendente(Object patrimonio) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCInstalacao",
					"this.CODBEM=?", new Object[] {patrimonio}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("AD_ENVIARPLANOGRAMA", "S");

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[marcarBotaoPendente] Nao foi possivel marcar o botao como pendente! codbem: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");;
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Estoque/";
			String objName = "J-Envio_verti_uppay";

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

			VO.setProperty("OBJETO", "btn_novoEnviarPlanograma");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
