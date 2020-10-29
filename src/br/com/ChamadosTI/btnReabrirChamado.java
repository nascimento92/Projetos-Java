package br.com.ChamadosTI;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceError;
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
		}else {
			throw new PersistenceError("Selecione apenas um chamado!");
		}
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		String status = (String) linhas[0].getCampo("STATUS");
		BigDecimal id = (BigDecimal) linhas[0].getCampo("ID");
		
		if("4".equals(status)) {	
			limpaResolucoes(id);
			reabrirOS(numos);
			alterarSubOs(numos);
			alteraInformacoesLocais(linhas);
		}else {
			throw new PersistenceError("Chamado não está concluído, não pode ser reaberto!");
		}
	}
	
	private void alteraInformacoesLocais(Registro[] linhas) throws Exception {
		linhas[0].setCampo("STATUS", "1");
		linhas[0].setCampo("DTFECHAMENTO", null);
	}
	
	private void limpaResolucoes(BigDecimal id) throws Exception {	
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_TRATATIVATI","this.ID=?", new Object[] {id}));
			
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("INIATIVIDADE", null);
				VO.setProperty("FIMATIVIDADE", null);
				VO.setProperty("TIPOATENDIMENTO", null);
				
				String descricaoAnterior = VO.asString("DESCRICAO");
				VO.setProperty("DESCRICAO", "CHAMADO REABERTO\n"+descricaoAnterior);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			System.out.println("## [btnReabrirChamado] ## - Nao foi possivel alterar as tratativas!");
			e.getMessage();
			e.getCause();
		}		
	}
	
	private void reabrirOS(BigDecimal numos) throws Exception {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico","this.NUMOS=?", new Object[] {numos}));
			
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("SITUACAO", "P");
				VO.setProperty("DTFECHAMENTO", null);
				VO.setProperty("CODUSUFECH", null);
				VO.setProperty("DHFECHAMENTOSLA", null);
				VO.setProperty("CODCOS", new BigDecimal(1));

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			System.out.println("## [btnReabrirChamado] ## - Nao foi possivel reabrir a OS!");
			e.getMessage();
			e.getCause();
		}
		
	}
	
	private void alterarSubOs(BigDecimal numos) throws Exception {
		
		try {
			
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
			
		} catch (Exception e) {
			System.out.println("## [btnReabrirChamado] ## - Nao foi possivel reabrir a sub-OS!");
			e.getMessage();
			e.getCause();
		}
		
	}
}
