package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_alterar_executante implements AcaoRotinaJava {
	
	int cont = 0;

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		Registro[] linhas = arg0.getLinhas();

		for (int i = 0; i < linhas.length; i++) {
			start(linhas[i], arg0);
		}
		
		if(cont>0) {
			arg0.setMensagemRetorno("Executante alterado");
		}
		

	}

	private void start(Registro linhas, ContextoAcao arg0) throws Exception {
		BigDecimal numos = new BigDecimal(0);
		BigDecimal numnovoresp = new BigDecimal(0);
		String novoresp = (String) arg0.getParam("NOVORESP");
		numos = (BigDecimal) linhas.getCampo("NUMOS");
		numnovoresp = new BigDecimal(novoresp);
		DynamicVO tabelaTcsite = getTcsite(numos);
		
		if(tabelaTcsite!=null) {
			BigDecimal codprod = tabelaTcsite.asBigDecimal("CODPROD");
			BigDecimal codserv = tabelaTcsite.asBigDecimal("CODSERV");
			BigDecimal codusurel = tabelaTcsite.asBigDecimal("CODUSU");
			
			cadastraServicoParaOhExecutante(numnovoresp, codprod, codserv);
			insertTcsrus(numnovoresp, codusurel);
			salvarNovoResponsavel(numos, numnovoresp);
			linhas.setCampo("RESPABAST", numnovoresp);
			
			this.cont++;
		}
		
		
	}

	public void salvarNovoResponsavel(BigDecimal NumOS, BigDecimal NovoResp) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> itensOS = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("ItemOrdemServico", "this.NUMOS=?", new Object[] { NumOS }));
			for (Iterator<?> Iterator = itensOS.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				VO.setProperty("CODUSU", NovoResp);
				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			System.out.println("[salvarNovoResponsavel] Não foi possível salvar o novo responsável a OS:" + NumOS + "\n"
					+ e.getMessage() + "\n" + e.getCause());
		}
	}

	private void cadastraServicoParaOhExecutante(BigDecimal usuario, BigDecimal produto, BigDecimal servico) {
		try {
			
			JapeWrapper dao = JapeFactory.dao("ServicoProdutoExecutante");
			DynamicVO servicoVO = dao.findOne("this.CODPROD=? AND this.CODUSU=? AND this.CODSERV=?", new Object[]{produto,usuario,servico});
			
			if(servicoVO==null) {
				dao.create().set("CODSERV", servico).set("CODUSU", usuario).set("CODPROD", produto).save();
			}
			
		} catch (Exception e) {
			System.out.println("[cadastraServicoParaOhExecutante] nfoi cadastrar o servi" + servico + " para o executante:"
					+ usuario + "\n" + e.getMessage() + "\n" + e.getCause());
		}
	}

	private DynamicVO getTcsite(BigDecimal NumOs) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ItemOrdemServico");
		DynamicVO VO = DAO.findOne("NUMOS=?", new Object[] { NumOs });
		return VO;
	}

	private void insertTcsrus(BigDecimal codusu, BigDecimal codusurel) throws Exception {
	    try {
	      
	    	JapeWrapper dao = JapeFactory.dao("RelacionamentoUsuario");
	    	DynamicVO relacionamentoVO = dao.findOne("this.CODUSU=? AND this.CODUSUREL=?", new Object[]{codusu,codusurel});
	    	
	    	if(relacionamentoVO==null) {
	    		dao
	    		.create()
	    		.set("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID())
	    		.set("CODUSUREL", codusurel)
	    		.set("TIPO", "G")
	    		.set("VINCULO", "S")
	    		.set("LIDERIMEDIATO", "N")
	    		.save();
	    	}
	    	
	    } catch (Exception e) {
	    	System.out.println("[insertTcsrus] não foi possível salvar na tcsrus usuario: "+codusu+"\n"+e.getMessage()+"\n"+e.getCause());
	    } 
	  }

		/*
		 * private void salvarException(String mensagem) { try { EntityFacade dwfFacade
		 * = EntityFacadeFactory.getDWFFacade(); EntityVO NPVO =
		 * dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS"); DynamicVO VO =
		 * (DynamicVO) NPVO; VO.setProperty("OBJETO", "btn_alterar_executante");
		 * VO.setProperty("PACOTE", "br.com.c.TelemetriaPropria");
		 * VO.setProperty("DTEXCEPTION", TimeUtils.getNow()); VO.setProperty("CODUSU",
		 * ((AuthenticationInfo)
		 * ServiceContext.getCurrent().getAutentication()).getUserID());
		 * VO.setProperty("ERRO", mensagem); dwfFacade.createEntity("AD_EXCEPTIONS",
		 * (EntityVO) VO); } catch (Exception e) { System.out.
		 * println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " +
		 * e.getMessage()); } }
		 */

}
