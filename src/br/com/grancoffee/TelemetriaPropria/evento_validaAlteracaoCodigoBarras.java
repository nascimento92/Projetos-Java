package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

import javax.swing.Timer;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class evento_validaAlteracaoCodigoBarras implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		salvaExcluido(arg0);		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
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
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		VO.setProperty("AD_INTEGRADOUPPAY", "N");
		
		Timer timer = new Timer(5000, new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				chamaPentaho();				
			}
		});
		timer.setRepeats(false);
		timer.start();
		
	}
	
	private void chamaPentaho() {
		
		try {
			final String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String site = parameter;
		    String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
		    WSPentaho si = new WSPentaho(site, Key);
		    		    
		    String path = "home/GC/Projetos/GCW/Transformations/";
		    String objName = "TF - GSN010 - Atualiza Codigo Barras";
		    
		    si.runTrans(path, objName);
		    		
		} catch (Exception e) {
			salvarException("[chamaPentaho] Não foi possível chamar a Rotina Pentaho!" + e.getMessage()+"\n"+e.getCause());
		}		
	}
	
	private void chamaPentahoExclusao() {

		try {
			final String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String site = parameter;
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Transformations/";
			String objName = "TF - GSN010 - Exclui Codigo Barras";

			si.runTrans(path, objName);

		} catch (Exception e) {
			salvarException(
					"[chamaPentaho] Não foi possível chamar a Rotina Pentaho!" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "evento_validaAlteracaoCodigoBarras");
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
	
	private void salvaExcluido(PersistenceEvent arg0) {
		try {
			DynamicVO VO = (DynamicVO) arg0.getVo();
			BigDecimal produto = VO.asBigDecimal("CODPROD");
			String codBarras = VO.asString("CODBARRA");
			
			if(produto.intValue()!=511836) {
				JapeWrapper barCode = JapeFactory.dao("CodigoBarras");
				barCode.create()
					.set("CODPROD",new BigDecimal(511836))
					.set("CODBARRA",codBarras)
					.set("AD_CODANT", produto)
					.save();
				
				Timer timer = new Timer(5000, new ActionListener() {	
					@Override
					public void actionPerformed(ActionEvent e) {
						chamaPentahoExclusao();				
					}
				});
				timer.setRepeats(false);
				timer.start();
			}
		
		} catch (Exception e) {
			salvarException("[salvaExcluido] Nao foi possivel salvar o produto excluido! "+e.getMessage()+"\n"+e.getCause());
		}
	}
}
