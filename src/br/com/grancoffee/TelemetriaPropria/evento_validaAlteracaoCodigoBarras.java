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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_validaAlteracaoCodigoBarras implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
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
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String codParaVenda = VO.asString("AD_CODBARVENDA");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		
		if("S".equals(codParaVenda)) {
			if(valida(produto)) {
				throw new Error("<br/><br/><b>Erro - Já existe um outro código de barras como principal para venda!</b><br/><br/>");
			}else {
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
		}
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		String codParaVenda = VO.asString("AD_CODBARVENDA");
		String oldcodParaVenda = oldVO.asString("AD_CODBARVENDA");
		
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		
		if("N".equals(oldcodParaVenda) || oldcodParaVenda==null) {
			if("S".equals(codParaVenda)) {
				if(valida(produto)) {
					throw new Error("<br/><br/><b>Erro - Já existe um outro código de barras como principal para venda!</b><br/><br/>");
				}else {
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
			}
		}	
	}
	
	private boolean valida(BigDecimal produto) {
		boolean valida=false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM TGFBAR WHERE AD_CODBARVENDA='S' AND CODPROD="+produto);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			salvarException("[valida] Nao foi possivel validar se existem mais de um cógido de barras marcado! "+e.getMessage()+"\n"+e.getCause());
		}
		
		return valida;
	}
	
	private void chamaPentaho() {
		
		try {
			
			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
		    String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
		    WSPentaho si = new WSPentaho(site, Key);
		    		    
		    String path = "home/GC/Projetos/GCW/Transformations/";
		    String objName = "TF - GSN010 - Atualiza Codigo Barras";
		    
		    si.runTrans(path, objName);
		    		
		} catch (Exception e) {
			salvarException("[chamaPentaho] Não foi possível chamar a Rotina Pentaho!" + e.getMessage()+"\n"+e.getCause());
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

}
