package br.com.TCIBEM;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_ajustarUltimoLocal implements AcaoRotinaJava {
	int cont = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		for (int i = 0; i < linhas.length; i++) {
			identificarPatrimonio(linhas[i], arg0);
		}
		
		if(cont>=1) {
			arg0.setMensagemRetorno("Patrimônios Ajustados!");
		}else {
			arg0.setMensagemRetorno("Ops, algo deu errado ! entrar em contato com o setor de TI!");
		}
	}

	public void identificarPatrimonio(Registro linhas, ContextoAcao arg0) {
		String patrimonio = (String) linhas.getCampo("CODBEM");
		BigDecimal produto = (BigDecimal) linhas.getCampo("CODPROD");
		ajustarUltimoLocal(patrimonio, produto);
	}

	public void ajustarUltimoLocal(String patrimonio, BigDecimal produto) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("BemNotafiscal",
					"this.CODBEM=?", new Object[] { patrimonio}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CODPROD", produto);

				itemEntity.setValueObject(NVO);
				cont++;
			}
		} catch (Exception e) {
			salvarException("[ajustarUltimoLocal] Nao foi possivel ajustar o ultimo local, patrimonio: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_ajustarUltimoLocal");
			VO.setProperty("PACOTE", "br.com.TCIBEM");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
