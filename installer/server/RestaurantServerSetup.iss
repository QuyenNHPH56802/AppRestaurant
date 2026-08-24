; PHASE 11 Inno Setup script for the Restaurant Server.
; Builds RestaurantServerSetup.exe from the jpackage app-image in target/jpackage/RestaurantServer.

#define MyAppName "Restaurant Server"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Restaurant Internal"
#define MyAppURL "https://restaurant.local"
#define MyAppExeName "RestaurantServer.exe"

[Setup]
AppId={{C5E1D3A7-1234-4567-89AB-CDEFA12BCDEF}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName={autopf}\Restaurant\RestaurantServer
DisableProgramGroupPage=yes
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog
OutputBaseFilename=RestaurantServerSetup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
UninstallDisplayIcon={app}\{#MyAppExeName}
SetupIconFile=icons\server.ico

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
BeveledLabel=Restaurant LAN Server v{#MyAppVersion}

[Files]
Source: "..\server\target\jpackage\RestaurantServer\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{autodesktop}\Restaurant Server Dashboard"; Filename: "http://localhost:8080/admin/"; Comment: "Open the dashboard in your browser"
Name: "{autodesktop}\Restaurant Server - Server Dashboard"; Filename: "http://localhost:8080/admin/server/"; Comment: "Server status and backups"
Name: "{autoprograms}\Restaurant\Restaurant Server Dashboard"; Filename: "http://localhost:8080/admin/"
Name: "{autoprograms}\Restaurant\Server Dashboard"; Filename: "http://localhost:8080/admin/server/"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Start the server now"; Flags: nowait postinstall skipifsilent
Filename: "http://localhost:8080/admin/"; Description: "Open the dashboard"; Flags: shellexec nowait postinstall skipifsilent unchecked

[UninstallRun]
; Stop the running server (best-effort) and remove the data directories
Filename: "{cmd}"; Parameters: "/C taskkill /F /IM {#MyAppExeName}"; Flags: runhidden
Filename: "{cmd}"; Parameters: "/C rmdir /S /Q ""{userappdata}\RestaurantServer"""; Flags: runhidden skipifdoesntexist

[Code]
// PHASE 11: Windows Firewall inbound rule for port 8080 (Private profile only).
// We run netsh advfirewall so the customer's installer auto-allows the LAN server.
procedure AddFirewallRule;
var
  ResultCode: Integer;
begin
  Exec('netsh', 'advfirewall firewall add rule name="Restaurant Server 8080 (Private)" dir=in action=allow protocol=TCP localport=8080 profile=private', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

procedure RemoveFirewallRule;
var
  ResultCode: Integer;
begin
  Exec('netsh', 'advfirewall firewall delete rule name="Restaurant Server 8080 (Private)"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then AddFirewallRule;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then RemoveFirewallRule;
end;

function InitializeSetup(): Boolean;
begin
  // Always allow install on supported Windows versions
  Result := True;
end;

function NeedRestart(): Boolean;
begin
  Result := False;
end;