package br.com.teclas;

import java.math.BigDecimal;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_importarSelecionadas implements AcaoRotinaJava {
	/**
	 * 22/10/20 09:10 - vs 1.0 - Criado para importar apenas as teclas selecionadas, AD_TECLASIMPORTADAS (Teclas Importadas).
	 * 26/01/21 16:23 - vs 1.1 - Inserido método para registrar log das Exceptions.
	 * 15/02/21 09:06 - vs 1.2 - Inserido método para excluir teclas, resolvendo o problema de duplicidade.
	 * 08/08/23 09:09 - vs 1.4 - Ajustes em toda a rotina a fim de correção de erros.
	 */

	int cont=0;
	
	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		start(contexto);
		
		if(cont>0) {
			contexto.setMensagemRetorno("Foram Inserida(s) / Alterada(s) <b>"+cont+"</b> Tecla(s)!");
		}else {
			throw new Error("Algo deu errado, verificar no log do sistema!");
		}
		
	}
	
	private void start(ContextoAcao contexto) throws Exception{
		Registro[] linhas = contexto.getLinhas();
		
		for(int i=0; i<linhas.length; i++) {
			inserirTecla(linhas[i], contexto);
		}
	}
	
	private void inserirTecla(Registro linhas,ContextoAcao contexto) throws Exception {
		String patrimonio = (String) linhas.getCampo("CODBEM");
		BigDecimal contrato = (BigDecimal) linhas.getCampo("NUMCONTRATO");
		BigDecimal tecla = (BigDecimal) linhas.getCampo("TECLA");
		BigDecimal produto = (BigDecimal) linhas.getCampo("CODPROD");
		
		if(!"S".equals(validaSeEhMicroMarketing(patrimonio))){ //Máquinas normais
			if(tecla.intValue()==0) {
				throw new Error("<br/> O patrimônio "+patrimonio+" não é uma loja, não pode ser inserida tecla 0");
			}
			
			ajustesMaquina(contrato, patrimonio, tecla, linhas);
			
		}
		else { //Lojas
			
			if(tecla.intValue()!=0) { //loja porém as teclas estão cadastradas numeradas
				tecla = new BigDecimal(0);
			}
			
			ajusteLoja(contrato, patrimonio, tecla, produto, linhas);
		}
		
		if(cont>0) { 
			linhas.setCampo("INSERIDA", "S"); 
			linhas.setCampo("DTALTERACAO",TimeUtils.getNow()); 
			linhas.setCampo("CODUSU", contexto.getUsuarioLogado());
		}
		
	}
	
	private void ajustesMaquina(BigDecimal contrato, String patrimonio, BigDecimal tecla, Registro linhas) throws Exception {
		if(verificaSeExisteAhTeclaMaquina(contrato,patrimonio,tecla)) { //existe uma tecla
			if(excluirTeclaMaquina(contrato, patrimonio, tecla)) {
				cadastrarTecla(linhas, contrato, tecla, patrimonio);
			}
		}else {
			cadastrarTecla(linhas, contrato, tecla, patrimonio);
		}
	}
	
	private void ajusteLoja(BigDecimal contrato, String patrimonio, BigDecimal tecla, BigDecimal produto, Registro linhas) throws Exception {
		if(verificaSeExisteAhTeclaLoja(contrato, patrimonio, produto)) {
			if(excluirTeclaLoja(contrato, patrimonio, produto)) {
				cadastrarTecla(linhas, contrato, tecla, patrimonio);
			}
		}else {
			cadastrarTecla(linhas, contrato, tecla, patrimonio);
		}
	}
	
	
	private boolean excluirTeclaMaquina(BigDecimal contrato, String codbem, BigDecimal tecla) throws Exception {
		boolean excluida = false;
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("teclas", "NUMCONTRATO=? AND CODBEM=? AND TECLA=?",
					new Object[] { contrato, codbem, tecla }));

			excluida = true;
		} catch (Exception e) {
			salvarException("[excluirTeclaMaquina] nao foi possivel excluir a tecla: " + tecla + " do patrimonio: "
					+ codbem + "\n" + e.getMessage() + "\n" + e.getCause());
		}
		return excluida;
	}
	
	private boolean excluirTeclaLoja(BigDecimal contrato, String codbem, BigDecimal produto) throws Exception {
		boolean excluida = false;
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("teclas", "NUMCONTRATO=? AND CODBEM=? AND CODPROD=?",
					new Object[] { contrato, codbem, produto }));

			excluida = true;
		} catch (Exception e) {
			salvarException("[excluirTeclaMaquina] nao foi possivel excluir o produto: " + produto + " do patrimonio: "
					+ codbem + "\n" + e.getMessage() + "\n" + e.getCause());
		}
		return excluida;
	}
	
	
	private boolean verificaSeExisteAhTeclaMaquina(BigDecimal contrato, String codbem, BigDecimal tecla) throws Exception{
		boolean existe = false;
		JapeWrapper DAO = JapeFactory.dao("teclas");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND CODBEM=? AND TECLA=?", new Object[] { contrato, codbem, tecla});
		if (VO != null) {
			return existe = true;
		}
		return existe;
	}
	
	private boolean verificaSeExisteAhTeclaLoja(BigDecimal contrato, String codbem, BigDecimal produto) throws Exception{
		boolean existe = false;
		JapeWrapper DAO = JapeFactory.dao("teclas");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND CODBEM=? AND CODPROD=?", new Object[] { contrato, codbem, produto});
		if (VO != null) {
			return existe = true;
		}
		return existe;
	}
		
	private void cadastrarTecla(Registro linhas,BigDecimal contrato, BigDecimal tecla, String patrimonio) throws Exception{
				
		BigDecimal produto = (BigDecimal) linhas.getCampo("CODPROD");
		BigDecimal vlrpar = BigDecimalUtil.getValueOrZero((BigDecimal) linhas.getCampo("VLRPARC"));
		BigDecimal vlrfun = BigDecimalUtil.getValueOrZero((BigDecimal) linhas.getCampo("VLRFUNC"));
		BigDecimal capacidade = BigDecimalUtil.getValueOrZero((BigDecimal) linhas.getCampo("CAPACIDADE"));
		BigDecimal nivelpar = BigDecimalUtil.getValueOrZero((BigDecimal) linhas.getCampo("NIVELPAR"));
		BigDecimal nivelalerta = BigDecimalUtil.getValueOrZero((BigDecimal) linhas.getCampo("NIVELALERTA"));
		String teclaAlternativa = (String) linhas.getCampo("TECLAALT");
		
		if(capacidade.intValue()==0) {
			capacidade = new BigDecimal(1);
		}
		
		if(nivelpar.intValue()==0) {
			nivelpar = new BigDecimal(1);
		}
	
		JapeWrapper teclaDAO = JapeFactory.dao("teclas");	
		DynamicVO save = teclaDAO.create()
		.set("NUMCONTRATO", contrato)
		.set("CODBEM", patrimonio)
		.set("TECLA", tecla)
		.set("CODPROD", produto)
		.set("VLRPAR", vlrpar)
		.set("VLRFUN", vlrfun)
		.set("AD_CAPACIDADE", capacidade)
		.set("AD_NIVELPAR", nivelpar)
		.set("AD_NIVELALERTA", nivelalerta)
		.save();
		
		if(teclaAlternativa!=null) {
			teclaDAO.prepareToUpdate(save).set("TECLAALT", teclaAlternativa).update();
		}
		
		this.cont++;
	}
	
	private String validaSeEhMicroMarketing(String patrimonio) throws Exception {
		String micromarketing = "N";
		
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		
		if(VO!=null) {
			 micromarketing = VO.asString("TOTEM");
		}

		return micromarketing;
	}
	
	private void salvarException(String mensagem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_importarSelecionadas");
			VO.setProperty("PACOTE", "br.com.teclas");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
