package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class evento_validaAlteracaoImagemProduto implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		insert(arg0);		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO newProduto = (DynamicVO) arg0.getVo();
		DynamicVO oldProduto = (DynamicVO) arg0.getOldVO();
		
		String newImagem = newProduto.asString("AD_PICURL");
		String oldImagem = oldProduto.asString("AD_PICURL");
		
		if(newImagem!=null) {
			if(newImagem!=oldImagem) {
				
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
	
	private void insert(PersistenceEvent arg0) {
		DynamicVO newProduto = (DynamicVO) arg0.getVo();
		String newImagem = newProduto.asString("AD_PICURL");
		if(newImagem!=null) {
			chamaPentaho();
		}
	}
	
	private void chamaPentaho() {

		try {
			final String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String site = parameter;
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Transformations/";
			String objName = "TF - GSN010 - Atualiza imagem prod.";

			si.runTrans(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}

}
