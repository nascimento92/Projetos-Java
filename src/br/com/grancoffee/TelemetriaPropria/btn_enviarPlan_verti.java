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

public class btn_enviarPlan_verti implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();		
		String patrimonio = (String) linhas[0].getCampo("CODBEM");
		
		DynamicVO instalacaoVO = getGcInstalacao(patrimonio);
		String micromarketing = instalacaoVO.asString("TOTEM");
		
		boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!", "Não pode ter <b>NENHUM</b> planograma ou picklist pendente para o bem! Continuar?", 0);
		
		if(confirmarSimNao) {
			if("S".equals(micromarketing)) {
				String body = montarBody(patrimonio);
				cadastrarTeclas(patrimonio,body);
			}else {
				if(validaSeExistemTeclasDuplicadas(patrimonio)) {
					arg0.mostraErro("<br/><b>Existem teclas repetidas! não é possível continuar</b><br/>");
				}else {
					String body = montarBody(patrimonio);
					cadastrarTeclas(patrimonio,body);
				}
			}
			
			arg0.setMensagemRetorno("Envio do planograma realizado!");
		}	
		
		Timer timer = new Timer(5000, new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				chamaPentaho();	
				//TODO :: Colocar para o pentaho, salvar no endereçamento o ID do planograma gerado
			}
		});
		timer.setRepeats(false);
		timer.start();
		
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
			throw new Error("[cadastrarTeclas] Ops! "+e.getMessage()+" "+e.getCause());
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
			throw new Error("[validaSeExistemTeclasDuplicadas] Ops! "+e.getMessage()+" "+e.getCause());
		}
		
		return valida;
	}
	
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
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		BigDecimal idVerti = VO.asBigDecimal("AD_IDPROVERTI");
		
		if(idVerti==null) {
			id = new BigDecimal(179707);
		}else {
			id = idVerti;
		}
		
		return id;
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Serving/Verti/";
			String objName = "J-Enviar_plan_verti";

			si.runJob(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}
}
