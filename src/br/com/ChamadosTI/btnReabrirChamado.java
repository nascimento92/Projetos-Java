package br.com.ChamadosTI;

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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnReabrirChamado implements AcaoRotinaJava {

	public void doAction(ContextoAcao arg0) throws Exception {
		
		Registro[] linhas = arg0.getLinhas();
		
		if(linhas.length==1) {
			start(linhas,arg0);
		}
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		String status = (String) linhas[0].getCampo("STATUS");
		
		if("CONCLUIDO".equals(status)) {
			alteraInformacoesLocais(linhas);
			deleteResolucoes(numos);
			reabrirOS(numos);
			alterarSubOs(numos);
		}
	}
	
	private void alteraInformacoesLocais(Registro[] linhas) throws Exception {
		linhas[0].setCampo("STATUS", "PENDENTE");
		linhas[0].setCampo("DTFECHAMENTO", null);
	}
	
	private void deleteResolucoes(BigDecimal numos) throws Exception {		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		dwfFacade.removeByCriteria(new FinderWrapper("AD_HISTCHAMADOSTI", "NUMOS=?", new Object[] { numos }));	
	}
	
	private void reabrirOS(BigDecimal numos) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico","this.NUMOS=?", new Object[] {numos}));
		
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("SITUACAO", "P");
			VO.setProperty("DTFECHAMENTO", null);
			VO.setProperty("CODCOS", new BigDecimal(1));

			itemEntity.setValueObject(NVO);
		}
	}
	
	private void alterarSubOs(BigDecimal numos) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico","this.NUMOS=?", new Object[] {numos}));
		
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("HRINICIAL", null);
			VO.setProperty("HRFINAL", null);
			VO.setProperty("INICEXEC", null);
			VO.setProperty("TERMEXEC", null);
			VO.setProperty("TEMPGASTO", null);
			VO.setProperty("CODSIT", new BigDecimal(1));

			itemEntity.setValueObject(NVO);
		}
	}
}
