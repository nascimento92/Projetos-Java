package br.com.buttons;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class btnMudarBox implements AcaoRotinaJava {
	
	private String codbem="";

	public void doAction(ContextoAcao arg0) throws Exception {

		String site = (String) MGECoreParameter.getParameter("URLVERTI");
		String Key = (String) MGECoreParameter.getParameter("TOKENVERTI");

		try {

			WServiceInvoker si = new WServiceInvoker(site, Key);

			// Parametro
			String idBox = (String) arg0.getParam("BOX");

			// verifica se está com somente uma linha selecionada
			int qtdLinhas = arg0.getLinhas().length;

			if (qtdLinhas > 1) {
				arg0.mostraErro("\n\n<b>Selecione apenas uma linha!</b>\n\n");
				return;
			}

			// Guarda toda linha em um vetor
			Registro[] linhas = arg0.getLinhas();

			// Pega um campo da Linha
			codbem = (String) linhas[0].getCampo("CODBEM");
			String idq = (String) arg0.getParam("BOX"); // perfumaria
		
			//marcarBlueToothAtivo(codbem, numcontrato);
			
			// Verifica os dados da tabela
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO codBemVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("PATRIMONIO", codbem); 
			DynamicVO labelBoxVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("INTEQUIP", idq);// perfumaria

			// Guarda o IDMACHINE e INSTALLATION_ID do Patrimonio que procuramos
			// em variaveis
			Object IDMACHINE = codBemVO.getProperty("IDMACHINE");
			Object INSTALLATION_ID = codBemVO.getProperty("INSTALLATION_ID");
			Object LABEL_NUMBER = labelBoxVO.getProperty("LABEL_NUMBER");// perfumaria

			// validação da idmachine e do installation
			if (IDMACHINE == null) {
				arg0.mostraErro("IDMACHINE do patrimonio: " + codbem
						+ " está nulo");
				return;
			} else if (INSTALLATION_ID == null) {
				arg0.mostraErro("INSTALLATION_ID do patrimonio: " + codbem
						+ " está nulo");
				return;
			}

			/*// Tudo o que é passado para o VERT neste caso com bluetooth ativo
			String metodo = "PATCH";
			String body = "{" + "\"installation\": {" + "\"equipment_id\": "
					+ idBox + "," + "\"enable_bluetooth\": true}}";*/
			
			// Tudo o que é passado para o VERT
						String metodo = "PATCH";
						String body = "{" + "\"installation\": {" + "\"equipment_id\": "+idBox+"}}";

			si.call(metodo, body, "machines", IDMACHINE.toString(),"installations", INSTALLATION_ID.toString());

			BigDecimal ID = new BigDecimal(idBox);

			linhas[0].setCampo("IDEQUIP", ID);

			arg0.setMensagemRetorno("DEU CERTO, Patrimonio: " + codbem
					+ " Nova Box: " + LABEL_NUMBER.toString() + " ID DA BOX: "
					+ ID.toString()); // perfumaria
			
			

		} catch (Exception e) {
		
			arg0.setMensagemRetorno("ERRO!! \n\n"+e.getMessage());
		}

	}

}
