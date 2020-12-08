package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;

import javax.swing.Timer;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
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

public class evento_cadastrarLojaUppay implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		beforeInsertUpdate(arg0);		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		beforeInsertUpdate(arg0);	
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
	
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {

	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
				
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
				
	}
	
	private void beforeInsertUpdate(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idTel = VO.asBigDecimal("IDTEL");
		String principal = VO.asString("PRINCIPAL");
		
		String patrimonio = VO.asString("CODBEM");
		DynamicVO gcInstalacao = getGcInstalacao(patrimonio);
		String micromarketing = gcInstalacao.asString("TOTEM");
		
		if(idTel.intValue()==2 && "S".equals(principal) && "S".equals(micromarketing)) {
			Timer timer = new Timer(20000, new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent e) {
					cadastrarLojaUppay();				
				}
			});
			timer.setRepeats(false);
			timer.start();
		}
	}
	
	private void cadastrarLojaUppay() {
		
		try {
			
			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
		    String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
		    WSPentaho si = new WSPentaho(site, Key);
		    		    
		    String path = "home/GC/Projetos/GCW/Transformations/";
		    String objName = "TF - GSN009 - Criar Loja uppay";
		    
		    si.runTrans(path, objName);
		    		
		} catch (Exception e) {
			salvarException("[cadastrarLojaUppay] Não foi possível chamar a Rotina Pentaho de Cadastro!" + e.getMessage()+"\n"+e.getCause());
		}		
	}
	
	private DynamicVO getGcInstalacao(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	
	
	private void chamaPentahoPlanograma() {

		try {

			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Transformations/";
			String objName = "TF - GSN009 - Enviar Planograma";
			String objName2 = "TF - GSN009 - Enviar Planograma Loja Uppay";

			si.runTrans(path, objName);
			si.runTrans(path, objName2);

		} catch (Exception e) {
			salvarException("[chamaPentahoPlanograma] Não foi possível chamar a Rotina Pentaho de Cadastro!" + e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "evento_cadastrarLojaUppay");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}
	
}
