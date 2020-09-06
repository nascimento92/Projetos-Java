package br.com.ReposicaoDeProdutos;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnAjusteDeEstoque implements AcaoRotinaJava {


	/**
	 * <h1> Botão Contagem APP </h1>
	 * 
	 * Objeto que pega as informações da tela App Inventário e insere na tela reposição de produtos.
	 * 
	 * @author gabriel.nascimento
	 */
	public void doAction(ContextoAcao arg0) throws Exception {

		BigDecimal contagem = new BigDecimal(0);

		/**
		 * Pega o código da reposição que será usado para validações
		 */
		Registro[] linha = arg0.getLinhas();
		BigDecimal codigo = (BigDecimal) linha[0].getCampo("CODIGO");

		/**
		 * Pega o código da contagem do APP (Digitdo pelo usuário)
		 * 
		 */
		Integer codContagem = (Integer) arg0.getParam("CONTAGEMAPP");

		/**
		 * Verifica na tabela contagem de estoque os dados correspondentes ao código digitado.
		 */
		JapeWrapper contagemDAO = null;
		contagemDAO = JapeFactory.dao("ContagemEstoqueProduto");
		DynamicVO contagemAPP = null;
		contagemAPP = contagemDAO.findOne(" CODCONTAGEM=?", new Object[] { codContagem });

		/**
		 *  - Primeiro IF ele valida se o código da contagem existe na tabela.
		 *  - Segundo IF ele valida se o código do local de estoque é igual na reposição e na contagem.
		 *  
		 *  Se as validações passagem ele começa com o procedimento
		 *  
		 *  1° - Procura da tabela de reposição os itens que correspondem a reposição atual.
		 *  2° - Guarda esses itens em uma coleção e itera os elementos através do FOR percorrendo cada um deles.
		 *  3° - A cada item ele procura se esse mesmo item existe na tebela da contagem se existe ele pega o valor digitado no app e joga no valor da tela de reposição.
		 *  
		 */
		if (contagemAPP != null) {

			if (validaLocal(codigo, codContagem)) {

				try {

					EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
					Collection<?> itensREPO = dwfEntityFacade.findByDynamicFinder(
							new FinderWrapper("TGFREPOEST", "this.CODIGO = ? ", new Object[] { codigo }));
					Collection<?> itensAPP = dwfEntityFacade.findByDynamicFinder(new FinderWrapper(
							"ContagemEstoqueProduto", "this.CODCONTAGEM = ? ", new Object[] { codContagem }));
					
					/**
					 * Esse método zera todas as quantidades dos produtos antes de substituir pelos valores corretos.
					 */
					zeraContagem(itensREPO, contagem);
					
					/**
					 * Inicio das iterações das coleções de itens das telas.
					 */
					for (Iterator<?> iteratorREPO = itensREPO.iterator(); iteratorREPO.hasNext();) { //primeiro for (tela reposição)

						PersistentLocalEntity itemEntity = (PersistentLocalEntity) iteratorREPO.next();
						DynamicVO itemVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
								.wrapInterface(DynamicVO.class);

						for (Iterator<?> iteratorAPP = itensAPP.iterator(); iteratorAPP.hasNext();) { //segundo for (tela contagem APP)
							
							PersistentLocalEntity itemAPPEntity = (PersistentLocalEntity) iteratorAPP.next();
							DynamicVO itemAppVO = (DynamicVO) ((DynamicVO) itemAPPEntity.getValueObject())
									.wrapInterface(DynamicVO.class);

							if (itemVO.getProperty("CODPROD").toString()
									.equals(itemAppVO.getProperty("CODPROD").toString())) { //If para saber se os produtos são iguais

								itemVO.setProperty("CONTAGEM", itemAppVO.getProperty("QTD")); //substituição dos itens tela app para reposição
								itemEntity.setValueObject((EntityVO) itemVO); //commit, sem isso não funciona !!
								
								gravaInformacoes(codContagem, linha); //grava na tela reposição dizendo que foram informações do app
								
								arg0.setMensagemRetorno("Dados carregados com sucesso!");
							}
						}

					}

				} catch (Exception e) {
					arg0.mostraErro("Erro:" + e.getMessage());
				}

			} else {
				arg0.mostraErro("Local de estoque da reposição não corresponde ao local de estoque da contagem!");
			}

		} else {
			arg0.mostraErro("Código de contagem inválida!");
		}

	}

	/**
	 * Método que zera todos os valores dos itens da tela de reposição.
	 * @param colecao
	 * @param valor
	 * @throws PersistenceException
	 */
	public void zeraContagem(Collection<?> colecao, BigDecimal valor) throws PersistenceException {

		for (Iterator<?> iteratorREPO = colecao.iterator(); iteratorREPO.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) iteratorREPO.next();
			DynamicVO itemVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			itemVO.setProperty("CONTAGEM", valor);
			itemEntity.setValueObject((EntityVO) itemVO);
		}
	}

	/**
	 * Método que valida se os locais de estoque da tela de reposição e contagem de app são iguais.
	 * @param codigo
	 * @param codContagem
	 * @return
	 * @throws Exception
	 */
	public boolean validaLocal(BigDecimal codigo, Integer codContagem) throws Exception {

		DynamicVO tabelaREPO = tabelaReposicao(codigo);
		DynamicVO tabelaAPP = tabelaContagem(codContagem);

		if (tabelaREPO.getProperty("CODLOCAL").equals(tabelaAPP.getProperty("CODLOCAL"))) {
			return true;
		} else {
			return false;
		}

	}
	
	/**
	 * Método que grava as informações do abastecedor, a data que foi feito a contagem e se ela foi feita por app.
	 * @param codigo
	 * @param codContagem
	 * @throws Exception
	 */
	public void gravaInformacoes(Integer codContagem, Registro[] linha) throws Exception {
		
		DynamicVO tabelaAPP = tabelaContagem(codContagem);
		
		linha[0].setCampo("AD_ABASTECEDOR", tabelaAPP.getProperty("CODUSU"));
		linha[0].setCampo("AD_FEITOPELOAPP", "S");
		linha[0].setCampo("AD_DATAINVENTARIO", tabelaAPP.getProperty("DHCONTAGEM").toString());
		linha[0].setCampo("CODCONTAGEMAPP", codContagem);
				
	}
	
	/**
	 * Método de apoio para identificar os dados da tabela de reposição
	 * @param codigo
	 * @return
	 * @throws Exception
	 */
	public DynamicVO tabelaReposicao(BigDecimal codigo) throws Exception {
		JapeWrapper tabelaDAO = null;
		tabelaDAO = JapeFactory.dao("TGFREPO");
		DynamicVO tabela = null;
		tabela = tabelaDAO.findOne(" CODIGO=?", new Object[] { codigo });
		return tabela;
	}
	
	/**
	 * Método de apoio para identificar os dados da tabela de contagem do APP
	 * @param codContagem
	 * @return
	 * @throws Exception
	 */
	public DynamicVO tabelaContagem(Integer codContagem) throws Exception {
		JapeWrapper tabela2DAO = null;
		tabela2DAO = JapeFactory.dao("ContagemEstoqueAvancado");
		DynamicVO tabela2 = null;
		tabela2 = tabela2DAO.findOne(" CODCONTAGEM=?", new Object[] { codContagem });
		return tabela2;
	}
	

}

