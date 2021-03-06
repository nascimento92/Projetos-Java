package br.com.LoteDoBem;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
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

public class btn_ajustaLoteDoBem implements AcaoRotinaJava {
	
	int cont=0;
	String retorno="";
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}

	public void start(ContextoAcao arg0) throws Exception {

		Integer nunota = (Integer) arg0.getParam("P_NUNOTA");

		if (validaNotaNoSistema(nunota)) {

			validaBensDaNota(nunota);
			
			if(cont>0) {
				arg0.setMensagemRetorno("BENS DA NOTA CORRIGIDOS");
			}else {
				arg0.setMensagemRetorno("Erro:! "+retorno );
			}
			

		} else {
			arg0.setMensagemRetorno("NOTA INVALIDA!");
		}

	}

	public boolean validaNotaNoSistema(Integer nunota) throws Exception {
		boolean valida = false;
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?", new Object[] { nunota });
		if (VO != null) {
			valida = true;
		}
		return valida;
	}

	public void validaBensDaNota(Integer nunota) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("ItemNota", "this.NUNOTA = ? ", new Object[] { nunota }));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal quantidadeNegociada = DynamicVO.asBigDecimal("QTDNEG");

			setaLoteIgualPatrimonio(nunota, produto,quantidadeNegociada);
		}
	}
	
	public void setaLoteIgualPatrimonio(Integer nunota,BigDecimal produto, BigDecimal quantidadeNegociada) throws Exception {
		
		String patrimonio=null;
		
		if(quantidadeNegociada.intValue()>1) {
			patrimonio="99999";
		}else if(quantidadeNegociada.intValue()==1) {
			JapeWrapper DAO = JapeFactory.dao("BemNotafiscal");
			DynamicVO VO = DAO.findOne("NUNOTA=? and CODPROD=?", new Object[] { nunota,produto });
			if (VO != null) {
				patrimonio = VO.asString("CODBEM");
			}else {
				patrimonio="99999";
			}
		}
		insereNaTGFEST(nunota,patrimonio,produto);
		setaLoteIgualPatrimonio(nunota,produto,patrimonio);
		
	}
	
	public void setaLoteIgualPatrimonio(Integer nunota,BigDecimal produto,String patrimonio) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemNota",
					"this.NUNOTA=? AND this.CODPROD=? ", new Object[] { nunota, produto }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CONTROLE", patrimonio);
				VO.setProperty("CODLOCALORIG", new BigDecimal(1500));

				itemEntity.setValueObject(NVO);
				
				cont++;
			}
		} catch (Exception e) {
			salvarException("[setaLoteIgualPatrimonio] nao foi possivel inserir o lote no produto! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void insereNaTGFEST(Integer nunota, String patrimonio, BigDecimal produto) {
		
		if(patrimonio!="99999") {
			try {
				
				DynamicVO TGFCAB = 	TGFCAB(nunota);
					
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Estoque");
				DynamicVO VO = (DynamicVO) NPVO;

				VO.setProperty("CODEMP", TGFCAB.asBigDecimal("CODEMP"));
				VO.setProperty("CODLOCAL", new BigDecimal(1500));
				VO.setProperty("CODPROD", produto);
				VO.setProperty("CONTROLE", patrimonio);
				VO.setProperty("RESERVADO", new BigDecimal(0));
				VO.setProperty("ESTMIN", new BigDecimal(0));
				VO.setProperty("ESTMAX", new BigDecimal(0));
				VO.setProperty("ATIVO", "S");
				VO.setProperty("DTVAL", new Timestamp(new Date("01/01/2050").getTime()));
				VO.setProperty("TIPO", "P");
				VO.setProperty("CODPARC", new BigDecimal(0));
				VO.setProperty("DTFABRICACAO", new Timestamp(new Date("01/01/2000").getTime()));
				VO.setProperty("STATUSLOTE", "N");
				VO.setProperty("ESTOQUE", new BigDecimal(1));

				dwfFacade.createEntity("Estoque", (EntityVO) VO);
					
				} catch (Exception e) {
					salvarException("[insereNaTGFEST] nao foi possivel inserir na TGFEST! "+e.getMessage()+"\n"+e.getCause());
				}
		}
		
	}
	
	public DynamicVO TGFCAB(Integer nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?", new Object[] { nunota });
		return VO;
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_cadastrarLoja");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui n�o tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}

}
