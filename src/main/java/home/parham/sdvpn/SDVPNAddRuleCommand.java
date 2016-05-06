/*
 * In The Name Of God
 * ======================================
 * [] Project Name : ShakhSDVPN 
 * 
 * [] Package Name : home.parham.sdvpn
 *
 * [] Creation Date : 29-03-2016
 *
 * [] Created By : Parham Alvani (parham.alvani@gmail.com)
 * =======================================
*/

package home.parham.sdvpn;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;


@Command(scope = "sdvpn", name = "add-rule", description = "Add rule for dropping packet at hosts")
public class SDVPNAddRuleCommand extends AbstractShellCommand {
	@Override
	protected void execute() {
		SDVPN sdvpn = get(SDVPN.class);
		print(sdvpn.toString());
	}
}
